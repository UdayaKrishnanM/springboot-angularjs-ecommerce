package com.ecommerce.demo.controller;

// CHANGE 1: Fixed == string comparisons
// CHANGE 5: Removed local @ExceptionHandler — GlobalExceptionHandler handles it

import com.ecommerce.demo.dto.*;
import com.ecommerce.demo.exception.*;
import com.ecommerce.demo.model.CartItem;
import com.ecommerce.demo.model.Order;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.Review;
import com.ecommerce.demo.service.CartItemService;
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
@RequestMapping("/api/user")
public class UserAccessController {

    @Autowired private ReviewService reviewService;
    @Autowired private OrderService orderService;
    @Autowired private CartItemService cartItemService;
    @Autowired private ProductService productService;

    // ── Products ──────────────────────────────────────────

    @GetMapping("/getAllProducts")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> list = productService.getAllProducts();
        if (list.isEmpty()) throw new ProductNotFoundException("Product list is empty");
        return ResponseEntity.ok(list);
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

    @GetMapping("/getProductById/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    // ── Orders ────────────────────────────────────────────

    @PostMapping("/createOrder")
    public ResponseEntity<UserOrderResponse> createOrder(@RequestBody Order order) {
        return orderService.createOrder(order)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OrderNotFoundException("Could not create order"));
    }

    @DeleteMapping("/deleteOrder/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        // CHANGE 1: was (deleted != "Order ID Not Exists") — fixed
        String result = orderService.deleteOrder(id);
        if (!result.equals("Order ID Not Exists")) {
            return ResponseEntity.ok("Order id " + id + " deleted successfully");
        }
        throw new OrderNotFoundException("Order not found with id: " + id);
    }

    @GetMapping("/getAllOrdersByEmail/{email}")
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

    // ── Reviews ───────────────────────────────────────────

    @PostMapping("/createReview")
    public ResponseEntity<ReviewDTO> createReview(@RequestBody Review review) {
        return ResponseEntity.ok(reviewService.createReview(review));
    }

    @GetMapping("/getReviewByUserId/{id}")
    public ResponseEntity<List<ReviewDTO>> getReviewByUser(@PathVariable Long id) {
        List<ReviewDTO> reviews = reviewService.getMyReviews(id);
        if (reviews.isEmpty()) throw new ReviewNotFoundException("No reviews found for user: " + id);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/getReviewByProductId/{productId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    @DeleteMapping("/deleteReviewByProductId/{productId}")
    public ResponseEntity<String> deleteReviewByProduct(@PathVariable Long productId,
                                                         @RequestParam Long userId) {
        return ResponseEntity.ok(reviewService.deleteReviewByProductId(productId, userId));
    }

    @PutMapping("/updateReviewByProductId/{productId}")
    public ResponseEntity<ReviewDTO> updateReviewByProductId(@PathVariable Long productId,
                                                              @RequestParam Long userId,
                                                              @RequestParam(required = false) String comment,
                                                              @RequestParam(required = false) int rating) {
        return reviewService.updateReviewByProductId(productId, userId, comment, rating)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ReviewNotFoundException("Update failed"));
    }

    // ── Cart ─────────────────────────────────────────────

    @GetMapping("/myCartItems")
    public ResponseEntity<CartResponseDTO> getCartItems() {
        return ResponseEntity.ok(cartItemService.getCartItemByUserId());
    }

    @PostMapping("/addcartItems")
    public ResponseEntity<String> addCartItem(@RequestBody CartItem cartItem) {
        // CHANGE 1: was (msg == "Item added to cart successfully!") — fixed
        String msg = cartItemService.addCartItem(cartItem);
        if (msg.equals("Item added to cart successfully!")
                || msg.equals("Cart item updated with merged quantity!")
                || msg.startsWith("Cart updated")) {
            return ResponseEntity.ok(msg);
        }
        throw new CartItemNotFoundException(msg);
    }

    @PutMapping("/updateCartItemsById")
    public ResponseEntity<CartResponseDTO> updateCartItem(@RequestBody CartItem cartItem) {
        return cartItemService.updateCartItem(cartItem)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));
    }

    @DeleteMapping("/deleteCartItemById/{id}")
    public ResponseEntity<String> deleteCartItem(@PathVariable Long id,
                                                  @RequestBody CartItem cartItemUserDetails) {
        // CHANGE 1: was (deleted == "Deleted successfully") — fixed
        String result = cartItemService.deleteCartItem(id, cartItemUserDetails);
        if (result.equals("Deleted successfully")) {
            return ResponseEntity.ok("Cart item " + id + " deleted successfully");
        }
        throw new CartItemNotFoundException("Cart item not found with id: " + id);
    }
}
