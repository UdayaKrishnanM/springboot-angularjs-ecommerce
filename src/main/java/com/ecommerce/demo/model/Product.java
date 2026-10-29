package com.ecommerce.demo.model;


import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    private String description;

    private double price;

    private String category;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ProductSize> sizes;

//    private int stockQuantity;

    public Product() {
    }

	public List<ProductSize> getSizes() {
		return sizes;
	}


	public void setSizes(List<ProductSize> sizes) {
		this.sizes = sizes;
	}

	public Product(Long id, String name, String description, String category, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
//        this.stockQuantity = stockQuantity;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

//	public int getStockQuantity() {
//		return stockQuantity;
//	}
//
//	public void setStockQuantity(int stockQuantity) {
//		this.stockQuantity = stockQuantity;
//	}
    
    
    
}
