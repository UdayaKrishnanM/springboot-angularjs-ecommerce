package com.ecommerce.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDateTime orderDate;
    
    @OneToMany(mappedBy = "order", cascade=CascadeType.ALL)
    @JsonManagedReference
    private List<OrderItem> orderItems;
    
    private double totalAmount;

    private String status;
    
    public double calculateTotalAmount() {
    	return orderItems.stream()
    			.mapToDouble(OrderItem::calculateSubtotal)
    			.sum();
    }
    


    public Order(Long id, User user, LocalDateTime orderDate, double totalAmount) {
        this.id = id;
        this.user = user;
        this.orderDate = orderDate;
        this.totalAmount = totalAmount;
    }
    
    public Order() {
    }
	
    public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public LocalDateTime getOrderDate() {
		return orderDate;
	}
	
	public void setOrderDate(LocalDateTime orderDate) {
		this.orderDate = orderDate;
	}
	
	public List<OrderItem> getOrderItems() {
		return orderItems;
	}
	
	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
		this.totalAmount = calculateTotalAmount();
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
