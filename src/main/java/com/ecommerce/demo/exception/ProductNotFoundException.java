package com.ecommerce.demo.exception;


public class ProductNotFoundException extends RuntimeException{

	
	public ProductNotFoundException(String message) {
		super(message);
	}
  
}

