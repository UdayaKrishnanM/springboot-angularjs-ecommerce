package com.ecommerce.demo.service;

// CHANGE 8: Email notifications triggered on order events.
// When an order is created → confirmation email sent.
// When status updated to SHIPPED → shipping email sent.
// When status updated to DELIVERED → delivery email sent.

import com.ecommerce.demo.dto.*;
import com.ecommerce.demo.email.services.EmailServices;
import com.ecommerce.demo.exception.OrderNotFoundException;
import com.ecommerce.demo.exception.ProductNotFoundException;
import com.ecommerce.demo.model.*;
import com.ecommerce.demo.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private ProductSizeRepository productSizeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailServices emailServices; // CHANGE 8

    public List<UserOrderResponse> getAllOrdersByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new OrderNotFoundException("User not found: " + email));
        List<Order> orders = orderRepository.findByUserId(user.getId());
        if (orders.isEmpty()) throw new OrderNotFoundException("No orders found for: " + email);
        return orders.stream().map(this::toOrderResponse).collect(Collectors.toList());
    }

    public Optional<UserOrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::toOrderResponse);
    }

    public Optional<UserOrderResponse> createOrder(Order order) {
        String authenticatedEmail = getAuthenticatedEmail();
        User user = userRepository.findById(order.getUser().getId())
                .orElseThrow(() -> new OrderNotFoundException("User not found"));
        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new OrderNotFoundException("Access denied: Unauthorized order request");
        }
        if (order.getId() != null && orderRepository.findById(order.getId()).isPresent()) {
            throw new OrderNotFoundException("Order already exists");
        }
        // FRONTEND: server sets date/status so the client can't send a fake date or "PAID".
        order.setOrderDate(java.time.LocalDateTime.now());
        order.setStatus("PENDING");
        order.setUser(user);
        Optional<UserOrderResponse> response = createOrderMethod(order);

        // CHANGE 8: Send confirmation email after successful order creation
        response.ifPresent(r -> {
            try {
                emailServices.sendOrderConfirmation(
                        user.getEmail(), user.getUsername(),
                        order.getId(), r.getTotalAmount()
                );
            } catch (Exception e) {
                logger.warn("Order confirmation email failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        });

        return response;
    }

    public Optional<UserOrderResponse> updateOrderStatus(Long id, Order updateOrder) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));

        if (!updateOrder.getUser().getId().equals(existingOrder.getUser().getId())) {
            throw new OrderNotFoundException("Unable to access: user mismatch");
        }

        String previousStatus = existingOrder.getStatus();
        existingOrder.setStatus(updateOrder.getStatus());
        orderRepository.save(existingOrder);

        // CHANGE 8: Trigger email based on new status
        User user = existingOrder.getUser();
        try {
            if ("SHIPPED".equalsIgnoreCase(updateOrder.getStatus())
                    && !"SHIPPED".equalsIgnoreCase(previousStatus)) {
                emailServices.sendOrderShippedNotification(user.getEmail(), user.getUsername(), id);
            } else if ("DELIVERED".equalsIgnoreCase(updateOrder.getStatus())
                    && !"DELIVERED".equalsIgnoreCase(previousStatus)) {
                emailServices.sendOrderDeliveredNotification(user.getEmail(), user.getUsername(), id);
            }
        } catch (Exception e) {
            logger.warn("Order status email failed for order {}: {}", id, e.getMessage());
        }

        return Optional.of(toOrderResponse(existingOrder));
    }

    public String deleteOrder(Long id) {
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()) return "Order ID Not Exists";

        String authenticatedEmail = getAuthenticatedEmail();
        User user = userRepository.findById(order.get().getUser().getId())
                .orElseThrow(() -> new OrderNotFoundException("User not found"));
        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new OrderNotFoundException("Access denied: Unauthorized order request");
        }

        orderRepository.deleteById(order.get().getId());
        return "Deleted Order Successfully ID: " + id;
    }

    // ── Private helpers ───────────────────────────────────

    private String getAuthenticatedEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof UserDetails)
                ? ((UserDetails) principal).getUsername()
                : principal.toString();
    }

    private Optional<UserOrderResponse> createOrderMethod(Order order) {
        double totalAmount = 0.0;
        Map<String, Integer> productSizeQtyMap = new HashMap<>();

        for (OrderItem item : order.getOrderItems()) {
            String key = item.getProduct().getId() + "_" + item.getProductSize().getSize().name();
            productSizeQtyMap.merge(key, item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : productSizeQtyMap.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long productId = Long.valueOf(parts[0]);
            SizeType size = SizeType.valueOf(parts[1]);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            ProductSize selectedSize = product.getSizes().stream()
                    .filter(s -> s.getSize() == size).findFirst()
                    .orElseThrow(() -> new OrderNotFoundException("Size " + size + " not available for: " + product.getName()));
            if (selectedSize.getStockQuantity() < entry.getValue()) {
                throw new OrderNotFoundException("Insufficient stock for: " + product.getName() + " size: " + size);
            }
        }

        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            ProductSize selectedSize = product.getSizes().stream()
                    .filter(s -> s.getSize() == item.getProductSize().getSize()).findFirst()
                    .orElseThrow(() -> new OrderNotFoundException("Size not available"));
            item.setProductSize(selectedSize);
        }

        order = orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId()).orElseThrow();
            ProductSize selectedSize = product.getSizes().stream()
                    .filter(s -> s.getSize() == item.getProductSize().getSize()).findFirst().orElseThrow();
            selectedSize.setStockQuantity(selectedSize.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
            item.setPrice(product.getPrice());
            totalAmount += item.calculateSubtotal();
            item.setOrder(order);
        }

        order.setTotalAmount(totalAmount);
        orderItemRepository.saveAll(order.getOrderItems());

        UserOrderResponse response = new UserOrderResponse();
        response.setUser(new UserDTO(order.getUser().getUsername(), order.getUser().getEmail()));
        response.setItems(order.getOrderItems().stream().map(item -> {
            Product p = item.getProduct();
            return new ItemDTO(
                    new ProductDTO(p.getName(), p.getDescription(), p.getPrice(), p.getCategory(),
                            item.getProductSize().getSize().toString()),
                    item.getQuantity(), item.getPrice());
        }).collect(Collectors.toList()));
        response.setTotalAmount(totalAmount);
        response.setId(order.getId());                 // FRONTEND
        response.setUserId(order.getUser().getId());   // FRONTEND
        response.setStatus(order.getStatus());
        response.setOrderDate(order.getOrderDate());
        return Optional.of(response);
    }

    private UserOrderResponse toOrderResponse(Order order) {
        User user = order.getUser();
        List<ItemDTO> items = order.getOrderItems().stream().map(item -> {
            Product p = item.getProduct();
            return new ItemDTO(
                    new ProductDTO(p.getName(), p.getDescription(), p.getPrice(), p.getCategory(),
                            item.getProductSize().getSize().toString()),
                    item.getQuantity(), item.getPrice());
        }).collect(Collectors.toList());
        UserOrderResponse response = new UserOrderResponse(new UserDTO(user.getUsername(), user.getEmail()),
                items, order.getTotalAmount(), order.getStatus(), order.getOrderDate());
        response.setId(order.getId());        // FRONTEND
        response.setUserId(user.getId());     // FRONTEND
        return response;
    }
}
