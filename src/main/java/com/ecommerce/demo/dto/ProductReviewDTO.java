package com.ecommerce.demo.dto;

public class ProductReviewDTO {

	private String name;
	
	private String description;

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

	public ProductReviewDTO(String name, String description) {
		super();
		this.name = name;
		this.description = description;
	}
	
	
	
}
