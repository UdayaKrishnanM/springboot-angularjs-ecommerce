package com.ecommerce.demo.dto;

import java.time.LocalDateTime;

public class ReviewDTO {
    
	private Long id;
    
    private UserDTO user;
    
    private ProductReviewDTO product;
    
    private int rating;
    
    private String comment;
    
    private LocalDateTime reviewDate;

    public ReviewDTO() {
    }

    public ReviewDTO(Long id, UserDTO user, ProductReviewDTO product, int rating, String comment, LocalDateTime reviewDate) {
        this.id = id;
        this.user = user;
        this.product = product;
        this.rating = rating;
        this.comment = comment;
        this.reviewDate = reviewDate;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public ProductReviewDTO getProduct() {
        return product;
    }

    public void setProduct(ProductReviewDTO product) {
        this.product = product;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

	public LocalDateTime getReviewDate() {
		return reviewDate;
	}

	public void setReviewDate(LocalDateTime reviewDate) {
		this.reviewDate = reviewDate;
	}


}
