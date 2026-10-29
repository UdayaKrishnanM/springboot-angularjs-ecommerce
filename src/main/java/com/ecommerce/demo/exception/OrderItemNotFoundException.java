package com.ecommerce.demo.exception;

public class OrderItemNotFoundException extends RuntimeException {

	public OrderItemNotFoundException(String message) {
		super(message);
	}
	
}
