package ma.alten.alten_backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.alten.alten_backend.config.Messages;
import ma.alten.alten_backend.dto.ProductDto;
import ma.alten.alten_backend.enumeration.InventoryStatus;
import ma.alten.alten_backend.exceptions.TechnicalException;
import ma.alten.alten_backend.mapper.ProductMapper;
import ma.alten.alten_backend.model.Product;
import ma.alten.alten_backend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static ma.alten.alten_backend.util.constants.GlobalConstants.PRODUCT_NOT_FOUND;

@Service
@AllArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final EntityManager entityManager;
    private final Messages messages;

    @Transactional
    public ProductDto addProduct(ProductDto productDTO) {
        log.debug("Start service add product");
        String code = generateCode();
        productDTO.setCode(code);
        Product product = productMapper.toProduct(productDTO);
        Product savedProduct = productRepository.save(product);
        log.debug("End service addProduct");
        return productMapper.toProductDto(savedProduct);
    }

    private String generateCode() {
        String newCode;
        int nextCodeNumber;

        Optional<String> lastCodeOpt = productRepository.findTopByOrderByCodeDesc().map(Product::getCode);
        if (lastCodeOpt.isPresent()) {
            String lastCode = lastCodeOpt.get();
            nextCodeNumber = Integer.parseInt(lastCode.replaceAll("\\D+", "")) + 1;
        } else {
            nextCodeNumber = 1;
        }
        do {
            newCode = String.format("PRODUCT%03d", nextCodeNumber);
            nextCodeNumber++;
        } while (productRepository.existsByCode(newCode));

        return newCode;
    }

    public Page<ProductDto> getAllProducts(int page, int size, String searchByCode, String searchByName,
                                           String searchByCategory, String searchByInventoryStatus, String searchByPriceRange) {
        log.debug("Start service Get Products page: {} size: {} searchByCode: {} searchByName: {} searchByCategory: {} searchByInventoryStatus: {} searchByPriceRange: {}", page, size, searchByCode, searchByName, searchByCategory, searchByInventoryStatus, searchByPriceRange);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (searchByCode != null || searchByName != null || searchByCategory != null ||
                searchByInventoryStatus != null || searchByPriceRange != null) {
            products = filterProducts(searchByCode, searchByName, searchByCategory, searchByInventoryStatus, searchByPriceRange, pageable);
        } else {
            products = productRepository.findAllWithDeletedIsFalse(pageable);
        }

        List<ProductDto> productDTOs = products.getContent().stream()
                .map(productMapper::toProductDto)
                .toList();
        log.debug("End service getProductsByCriteria ");
        return new PageImpl<>(productDTOs, pageable, products.getTotalElements());
    }

    private Page<Product> filterProducts(String searchByCode, String searchByName, String searchByCategory,
                                         String searchByInventoryStatus, String searchByPriceRange, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
        Root<Product> root = criteriaQuery.from(Product.class);

        Predicate predicate = buildPredicate(criteriaBuilder, root, searchByCode, searchByName, searchByCategory, searchByInventoryStatus, searchByPriceRange);
        criteriaQuery.where(predicate);

        TypedQuery<Product> typedQuery = entityManager.createQuery(criteriaQuery);
        long totalCount = typedQuery.getResultList().size();
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<Product> resultList = typedQuery.getResultList();

        return new PageImpl<>(resultList, pageable, totalCount);
    }

    private Predicate buildPredicate(CriteriaBuilder criteriaBuilder, Root<Product> root,
                                     String searchByCode, String searchByName, String searchByCategory,
                                     String searchByInventoryStatus, String searchByPriceRange) {
        Predicate predicate = criteriaBuilder.conjunction();

        if (searchByCode != null) {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("code")),
                    "%" + searchByCode.toLowerCase() + "%"));
        }

        if (searchByName != null) {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                    "%" + searchByName.toLowerCase() + "%"));
        }

        if (searchByCategory != null) {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("category"), searchByCategory));
        }

        if (searchByInventoryStatus != null) {
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("inventoryStatus"), searchByInventoryStatus));
        }

        if (searchByPriceRange != null) {
            String[] range = searchByPriceRange.split("-");
            double minPrice = Double.parseDouble(range[0]);
            double maxPrice = Double.parseDouble(range[1]);
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.between(root.get("price"), minPrice, maxPrice));
        }

        return predicate;
    }


    public ProductDto updateProduct(Long id, ProductDto productDTO) throws TechnicalException {
        log.debug("Start service update product id {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new TechnicalException(messages.get(PRODUCT_NOT_FOUND)));

        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setImage(productDTO.getImage());
        product.setCategory(productDTO.getCategory());
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity());
        product.setInternalReference(productDTO.getInternalReference());
        product.setShellId(productDTO.getShellId());
        product.setInventoryStatus(InventoryStatus.valueOf(productDTO.getInventoryStatus()));
        product.setRating(productDTO.getRating());
        product.setUpdatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));
        log.debug("End service update product  with id {}, product {}", id, productDTO);
        return productMapper.toProductDto(productRepository.save(product));
    }

    public ProductDto getProductById(Long id) throws TechnicalException {
        log.debug("Start service get product By Id {}", id);
        return productRepository.findById(id)
                .map(productMapper::toProductDto)
                .orElseThrow(() -> new TechnicalException(messages.get(PRODUCT_NOT_FOUND)));
    }

    public void deleteProductPhysical(Long id) throws TechnicalException {
        log.debug("Start service delete physical product By Id {}", id);
        if (id == null) {
            throw new TechnicalException(messages.get(PRODUCT_NOT_FOUND));
        }
        productRepository.deleteById(id);
        log.debug("End service delete product By Id {}", id);
    }

    public void deleteProductLogical(Long id) throws TechnicalException {
        log.debug("Start service delete logical product By Id {}", id);
        if (id == null) {
            throw new TechnicalException(messages.get(PRODUCT_NOT_FOUND));
        }

        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();

            product.setDeleted(true);
            productRepository.save(product);
        }
        log.debug("End service delete product By Id {}", id);
    }

}
