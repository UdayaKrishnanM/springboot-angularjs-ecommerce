package com.ecommerce.demo.dto;

import java.util.*;

import com.ecommerce.demo.model.User;

public class CartResponseDTO {
    
	private UserDTO user;

    private List<CartItemResponseDTO> cartItems;

    
	public UserDTO getUser() {
		return user;
	}
	public void setUser(UserDTO user) {
		this.user = user;
	}
	public List<CartItemResponseDTO> getCartItems() {
		return cartItems;
	}
	public void setCartItems(List<CartItemResponseDTO> cartItems) {
		this.cartItems = cartItems;
	}
	public CartResponseDTO() {
		super();
	}
	public CartResponseDTO(UserDTO user, List<CartItemResponseDTO> cartItems) {
		super();
		this.user = user;
		this.cartItems = cartItems;
	}    
	
    
}