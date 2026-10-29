package com.ecommerce.demo.service;

// CHANGE 6: @Cacheable added to getProductById — results cached in Redis.
//            @CacheEvict added to updateProduct and deleteProduct — cache cleared
//            when data changes so users never get stale results.
//
// How it works in plain English:
//   First call to getProductById(5): hits MySQL, saves result in Redis with key "products::5"
//   Second call: Redis returns it instantly, MySQL never touched
//   Call to deleteProduct(5): @CacheEvict removes "products::5" from Redis
//   Next call to getProductById(5): cache miss → goes back to MySQL

import com.ecommerce.demo.dto.ProductDTO;
import com.ecommerce.demo.exception.ProductNotFoundException;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.ProductSize;
import com.ecommerce.demo.model.SizeType;
import com.ecommerce.demo.pagination.ProductSpecification;
import com.ecommerce.demo.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Page<ProductDTO> getFilteredProducts(String category, Double minPrice, Double maxPrice,
                                                 String name, int page, int size, String sortBy, String sortDir) {
        Specification<Product> spec = ProductSpecification.getFilteredProducts(category, minPrice, maxPrice, name);
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(spec, pageable).map(this::toDTO);
    }

    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO(product.getName(), product.getDescription(), product.getPrice(), product.getCategory());
        dto.setId(product.getId()); // FRONTEND: UI needs the id to open the product page
        return dto;
    }

    // CHANGE 6: Cache result by product ID — key is "products::{id}"
    @Cacheable(value = "products", key = "#id")
    public Optional<ProductDTO> getProductById(Long id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    public String addProduct(Product product) {
        Optional<Product> existing = productRepository.findByName(product.getName());
        if (existing.isPresent() && existing.get().getDescription().equalsIgnoreCase(product.getDescription())) {
            return "Product already added";
        }
        // FRONTEND FIX: ProductSize.product is @JsonIgnore, so sizes posted as JSON
        // arrive without their parent. Without this, sizes are saved with product_id = NULL
        // and the product can never be added to a cart.
        if (product.getSizes() != null) {
            product.getSizes().forEach(s -> s.setProduct(product));
        }
        productRepository.save(product);
        return "Product added successfully";
    }

    // CHANGE 6: Evict from cache when product is deleted
    @CacheEvict(value = "products", key = "#id")
    public String deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(p -> {
                    productRepository.deleteById(p.getId());
                    return "Deleted Successfully ID: " + id;
                })
                .orElse("Product Not Exists");
    }

    public int getStockQuantity(Product product, SizeType requestedSize) {
        return product.getSizes().stream()
                .filter(s -> s.getSize() == requestedSize)
                .map(ProductSize::getStockQuantity)
                .findFirst()
                .orElse(0);
    }

    // CHANGE 6: Evict from cache when product is updated
    @CacheEvict(value = "products", key = "#id")
    public Optional<Product> updateProduct(Long id, Product productDetails, SizeType requestedSize, int newStockQuantity) {
        return productRepository.findById(id).map(product -> {
            if (productDetails.getName() != null) product.setName(productDetails.getName());
            if (productDetails.getPrice() != 0) product.setPrice(productDetails.getPrice());
            if (productDetails.getDescription() != null) product.setDescription(productDetails.getDescription());
            if (productDetails.getCategory() != null) product.setCategory(productDetails.getCategory());

            product.getSizes().stream()
                    .filter(s -> s.getSize() == requestedSize)
                    .findFirst()
                    .ifPresent(s -> s.setStockQuantity(newStockQuantity));

            return productRepository.save(product);
        });
    }
}
