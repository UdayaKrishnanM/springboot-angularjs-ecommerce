package com.ecommerce.demo.controller;

// CHANGE 1: Fixed == string comparison bug. Java == compares object references,
// not string values. "abc" == "abc" can return false for different String objects
// even if they contain identical characters. Always use .equals().
//
// CHANGE 5: All local @ExceptionHandler methods REMOVED — GlobalExceptionHandler
// handles everything now. Keeping them here would cause conflicts.

import com.ecommerce.demo.dto.*;
import com.ecommerce.demo.exception.OrderNotFoundException;
import com.ecommerce.demo.exception.ProductNotFoundException;
import com.ecommerce.demo.exception.ReviewNotFoundException;
import com.ecommerce.demo.model.Order;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.service.OrderService;
import com.ecommerce.demo.service.ProductService;
import com.ecommerce.demo.service.ReviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminBasedController {

    @Autowired private ProductService productService;
    @Autowired private ReviewService reviewService;
    @Autowired private OrderService orderService;

    // ── Products ──────────────────────────────────────────

    @GetMapping("/getAllProducts")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> list = productService.getAllProducts();
        if (list.isEmpty()) throw new ProductNotFoundException("Product list is empty");
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getProductById/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @GetMapping("/filter")
    public Page<ProductDTO> filterProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return productService.getFilteredProducts(category, minPrice, maxPrice, name, page, size, sortBy, sortDir);
    }

    @PostMapping("/createProduct")
    public ResponseEntity<String> createProduct(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(product));
    }

    @PutMapping("/updateProduct/{id}")
    public ResponseEntity<Optional<Product>> updateProduct(@PathVariable Long id,
                                                            @RequestBody UpdateProductDTO dto) {
        return productService.updateProduct(id, dto.getProductDetails(), dto.getRequestedSize(), dto.getNewStockQuantity())
                .map(p -> ResponseEntity.ok(Optional.of(p)))
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    @DeleteMapping("/deleteProduct/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        // CHANGE 1: was (deleted != "Product Not Exists") — now uses .equals()
        String result = productService.deleteProduct(id);
        if (!result.equals("Product Not Exists")) {
            return ResponseEntity.ok("Product id " + id + " deleted successfully");
        }
        throw new ProductNotFoundException("Product not found with id: " + id);
    }

    // ── Orders ────────────────────────────────────────────

    @GetMapping("/getAllMyOrdersByEmail/{email}")
    public ResponseEntity<List<UserOrderResponse>> getAllOrders(@PathVariable String email) {
        List<UserOrderResponse> list = orderService.getAllOrdersByEmail(email);
        if (list.isEmpty()) throw new OrderNotFoundException("No orders found for: " + email);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getOrderById/{id}")
    public ResponseEntity<UserOrderResponse> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    @PutMapping("/updateOrderStatus/{id}")
    public ResponseEntity<UserOrderResponse> updateOrderStatus(@PathVariable Long id,
                                                                @RequestBody Order updateOrder) {
        return orderService.updateOrderStatus(id, updateOrder)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
    }

    // ── Reviews ───────────────────────────────────────────

    @DeleteMapping("/deleteReviewByProductId/{productId}")
    public ResponseEntity<String> deleteReviewByProduct(@PathVariable Long productId,
                                                         @RequestParam Long userId) {
        return ResponseEntity.ok(reviewService.deleteReviewByProductId(productId, userId));
    }

    @GetMapping("/getReviewById/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with id: " + id));
    }

    @GetMapping("/getAllReviews")
    public ResponseEntity<List<ReviewDTO>> getAllReviews() {
        List<ReviewDTO> reviews = reviewService.getAllReviews();
        if (reviews.isEmpty()) throw new ReviewNotFoundException("No reviews found");
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/getReviewByProductId/{productId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }
}
