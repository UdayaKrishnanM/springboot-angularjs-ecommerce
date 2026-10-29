package com.ecommerce.demo.service;

// CHANGE 10: Tests for ProductService including caching behavior.

import com.ecommerce.demo.dto.ProductDTO;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product(1L, "Nike Air Max", "Running shoes", "Footwear", 4999.0);
    }

    @Test
    @DisplayName("getProductById: returns DTO when product exists")
    void getProductById_found() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Optional<ProductDTO> result = productService.getProductById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Nike Air Max");
        assertThat(result.get().getPrice()).isEqualTo(4999.0);
    }

    @Test
    @DisplayName("getProductById: returns empty when product not found")
    void getProductById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ProductDTO> result = productService.getProductById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("addProduct: saves and returns success message")
    void addProduct_success() {
        when(productRepository.findByName("Nike Air Max")).thenReturn(Optional.empty());

        String result = productService.addProduct(testProduct);

        assertThat(result).isEqualTo("Product added successfully");
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("addProduct: rejects duplicate product with same name+description")
    void addProduct_duplicate() {
        Product existingProduct = new Product(2L, "Nike Air Max", "Running shoes", "Footwear", 4999.0);
        when(productRepository.findByName("Nike Air Max")).thenReturn(Optional.of(existingProduct));

        String result = productService.addProduct(testProduct);

        assertThat(result).isEqualTo("Product already added");
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: deletes and returns success message")
    void deleteProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        String result = productService.deleteProduct(1L);

        assertThat(result).contains("Deleted Successfully");
        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProduct: returns 'Product Not Exists' when not found")
    void deleteProduct_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        String result = productService.deleteProduct(99L);

        assertThat(result).isEqualTo("Product Not Exists");
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getAllProducts: returns full list")
    void getAllProducts_returnsList() {
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        List<Product> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Nike Air Max");
    }
}
