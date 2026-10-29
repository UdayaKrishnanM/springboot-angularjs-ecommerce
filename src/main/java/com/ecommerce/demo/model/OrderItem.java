package com.ecommerce.demo.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Product product;

    @ManyToOne
    @JsonBackReference
    private Order order;

    private int quantity;

    public double price;

	@ManyToOne
	@JoinColumn(name = "product_size_id", nullable = false)
	private ProductSize productSize;  // Linking to ProductSize

    public OrderItem(Order order, Product product, int quantity, double price) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }
    
    public double calculateSubtotal() {
    	return quantity * price;
    }
    
    public OrderItem() {
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
		this.price = product.getPrice();
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public double getPrice() {
		return price;
	}

	public ProductSize getProductSize() {
		return productSize;
	}

	public void setProductSize(ProductSize productSize) {
		this.productSize = productSize;
	}

	public void setPrice(double price) {
		this.price = price;
	}
    
    public double getSubtotal() {
    	return calculateSubtotal();
    }

	@Override
	public String toString() {
		return "OrderItem [id=" + id + ", product=" + product + ", order=" + order + ", quantity=" + quantity
				+ ", price=" + price + "]";
	}
    
    
    
}
