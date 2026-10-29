package com.ecommerce.demo.dto;

public class UserDTO{
	

	private String username;
	
	private String email;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	
	public UserDTO(String username, String email) {
		super();
		this.username = username;
		this.email = email;
	}


	public UserDTO() {
		super();
	}
	
	
	
}
