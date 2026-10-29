package com.ecommerce.demo.dto;

import java.time.LocalDateTime;
import java.util.*;

import com.ecommerce.demo.model.OrderItem;


public class UserOrderResponse {

	// FRONTEND: order id (needed for payment / cancel) and owner's user id
	// (needed by admin's updateOrderStatus, which checks user id).
	private Long id;
	private Long userId;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }

	private UserDTO user;
	
	private List<ItemDTO> items;
		
    private double totalAmount;

    private String status;
    
    private LocalDateTime orderDate;


	public LocalDateTime getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}

	public UserDTO getUser() {
		return user;
	}
	
	public void setUser(UserDTO user) {
		this.user = user;
	}
	
	public List<ItemDTO> getItems() {
		return items;
	}
	
	public void setItems(List<ItemDTO> items) {
		this.items = items;
	}
	
	

	public UserOrderResponse(UserDTO user, List<ItemDTO> items) {
		super();
		this.user = user;
		this.items = items;
	}

	public UserOrderResponse() {
		super();
	}

    
    public double calculateTotalAmount() {
    	return items.stream()
    			.mapToDouble(ItemDTO::calculateSubtotal)
    			.sum();
    }
	public void setOrderItems(List<OrderItem> orderItems) {
		this.items = items;
		this.totalAmount = calculateTotalAmount();
	}
	
		
	public UserOrderResponse(UserDTO user, List<ItemDTO> items, double totalAmount, String status,
			LocalDateTime orderDate) {
		super();
		this.user = user;
		this.items = items;
		this.totalAmount = totalAmount;
		this.status = status;
		this.orderDate = orderDate;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
    


	
}


