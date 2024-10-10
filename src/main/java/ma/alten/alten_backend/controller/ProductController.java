package ma.alten.alten_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.alten.alten_backend.dto.ProductDto;
import ma.alten.alten_backend.exceptions.TechnicalException;
import ma.alten.alten_backend.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product", description = "Crée un nouveau produit")
    @PostMapping( consumes = MediaType.MULTIPART_FORM_DATA_VALUE,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> addProduct(@RequestPart("product") ProductDto productDto, @RequestPart("imageFile") MultipartFile imageFile) throws IOException {
        log.info("Adding new Product: {}", productDto);
        ProductDto createdProduct = productService.addProduct(productDto, imageFile);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @Operation(summary = "Retrieve all products", description = "Récupère la liste de tous les produits")
    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) final String searchByCode,
            @RequestParam(required = false) final String searchByName,
            @RequestParam(required = false) final String searchByCategory,
            @RequestParam(required = false) final String searchByInventoryStatus,
            @RequestParam(required = false) final String searchByPriceRange
    ) {
        Page<ProductDto> products = productService.getAllProducts(page, size, searchByCode, searchByName, searchByCategory, searchByInventoryStatus, searchByPriceRange);
        return ResponseEntity.ok(products);
    }


    @Operation(summary = "Update product details", description = "Récupère les détails d'un produit par ID")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDTO) throws TechnicalException {
        log.info("Update product: {}", id);
        ProductDto updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }


    @Operation(summary = "Retrieve product by ID", description = "Récupère les détails d'un produit par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) throws TechnicalException {
        log.info("get product by id: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }


    @Operation(summary = "Delete a product physical", description = "Supprime un produit par ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductPhysical (@PathVariable Long id) throws TechnicalException {
        log.info("delete product by id: {}", id);
        productService.deleteProductPhysical(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Archive a product", description = "archiver un produit par ID")
    @DeleteMapping("archive/{id}")
    public ResponseEntity<Void> deleteProductLogical(@PathVariable Long id) throws TechnicalException {
        log.info("archiver product by id: {}", id);
        productService.deleteProductLogical(id);
        return ResponseEntity.noContent().build();
    }

}
