package com.ecommerce.demo.dto;

import com.ecommerce.demo.model.ProductSize;

import java.util.List;

public class ProductDTO{

	// FRONTEND: id added so the UI can link from the product list to a product page.
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	private String name;
	
	private String description;
	
	private double price;
	
	private String category;

	private String sizeType;


	public ProductDTO(String name, String description, double price, String category, String sizeType) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.category = category;
		this.sizeType = sizeType;
	}

	public String getSizeType() {
		return sizeType;
	}

	public void setSizeType(String sizeType) {
		this.sizeType = sizeType;
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


	public ProductDTO(String name, String description, double price, String category) {
		super();
		this.name = name;
		this.description = description;
		this.price = price;
		this.category = category;
	}

	public ProductDTO() {
		super();
	}
	
	
	
}