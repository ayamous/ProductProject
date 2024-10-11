package ma.alten.alten_backend.controller;

import ma.alten.alten_backend.dto.ProductDto;
import ma.alten.alten_backend.exceptions.TechnicalException;
import ma.alten.alten_backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productDto = new ProductDto();
        productDto.setId(1L);
        productDto.setName("Produit Test");
        productDto.setCategory("Category Test");
        productDto.setPrice(10.0);
    }

    @Test
    void addProduct_ShouldReturnCreatedProduct() throws IOException {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "image.jpg", "image/jpeg", new byte[0]);

        when(productService.addProduct(any(ProductDto.class), any(MultipartFile.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.addProduct(productDto, imageFile);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(productDto, response.getBody());
    }

    @Test
    void getAllProducts_ShouldReturnProductPage() {
        Page<ProductDto> productPage = new PageImpl<>(Collections.singletonList(productDto));
        when(productService.getAllProducts(0, 5, null, null, null, null, null)).thenReturn(productPage);

        ResponseEntity<Page<ProductDto>> response = productController.getAllProducts(0, 5, null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productPage, response.getBody());
    }

    @Test
    void updateProduct_ShouldReturnUpdatedProduct() throws TechnicalException {
        when(productService.updateProduct(anyLong(), any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.updateProduct(1L, productDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productDto, response.getBody());
    }

    @Test
    void getProductById_ShouldReturnProduct() throws TechnicalException {
        when(productService.getProductById(1L)).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.getProductById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(productDto, response.getBody());
    }

    @Test
    void deleteProductPhysical_ShouldReturnNoContent() throws TechnicalException {
        doNothing().when(productService).deleteProductPhysical(1L);

        ResponseEntity<Void> response = productController.deleteProductPhysical(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteProductLogical_ShouldReturnNoContent() throws TechnicalException {
        doNothing().when(productService).deleteProductLogical(1L);

        ResponseEntity<Void> response = productController.deleteProductLogical(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

}
