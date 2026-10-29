package com.ecommerce.demo.exception;

import com.ecommerce.demo.model.Order;

public class OrderNotFoundException extends RuntimeException{

	public OrderNotFoundException(String message) {
		super(message);
	}
	
	public OrderNotFoundException(Order order, String message) {
		super(message);
	}
	
}
