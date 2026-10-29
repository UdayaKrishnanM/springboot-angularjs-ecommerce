package com.ecommerce.demo.service;

import com.ecommerce.demo.dto.ProductDTO;
import com.ecommerce.demo.dto.ProductReviewDTO;
import com.ecommerce.demo.dto.ReviewDTO;
import com.ecommerce.demo.dto.UserDTO;
import com.ecommerce.demo.exception.OrderNotFoundException;
import com.ecommerce.demo.exception.ReviewNotFoundException;
import com.ecommerce.demo.model.Order;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.Review;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.repository.OrderRepository;
import com.ecommerce.demo.repository.ProductRepository;
import com.ecommerce.demo.repository.ReviewRepository;
import com.ecommerce.demo.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;
    
    private final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    
    public List<ReviewDTO> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();
        return reviews.stream()
                      .map(this::convertToReviewDTO)
                      .collect(Collectors.toList());
    }


	public List<ReviewDTO> getReviewsByProductId(Long productId) {
		List<Review> reviews = reviewRepository.findAllByProductId(productId);

		if (reviews.isEmpty()) {
			throw new ReviewNotFoundException("No reviews found for product ID: " + productId);
		}

		return reviews.stream().map(review -> {
			User user = review.getUser();
			Product product = review.getProduct();

			return new ReviewDTO(
					review.getId(),
					new UserDTO(user.getUsername(), user.getEmail()),
					new ProductReviewDTO(product.getName(), product.getDescription()),
					review.getRating(),
					review.getComment(),
					review.getReviewDate()
			);
		}).collect(Collectors.toList());
	}


	public List<ReviewDTO> getMyReviews(Long id) {
        List<Review> myReviews = reviewRepository.findByUserId(id);
        return myReviews.stream().map(
        		review -> {
        			User user = review.getUser();
        			UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail());
        			
        			Product product = review.getProduct();
        			ProductReviewDTO productDTO = new ProductReviewDTO(product.getName(), product.getDescription());       		
        			return new ReviewDTO(review.getId(), userDTO, productDTO, review.getRating(), review.getComment(), review.getReviewDate());
        		}
        		).collect(Collectors.toList());
        
        
    }

    public Optional<ReviewDTO> getReviewById(Long id) {
        Optional<Review> reviewOptional = reviewRepository.findById(id);
        return reviewOptional.map(this::convertToReviewDTO);
    }

    private ReviewDTO convertToReviewDTO(Review review) {
        UserDTO userDTO = new UserDTO(
            review.getUser().getUsername(),
            review.getUser().getEmail()
        );

        ProductReviewDTO productDTO = new ProductReviewDTO(
            review.getProduct().getName(),
            review.getProduct().getDescription()
        );

        return new ReviewDTO(
            review.getId(),
            userDTO,
            productDTO,
            review.getRating(),
            review.getComment(),
            review.getReviewDate()
        );
    }



	public ReviewDTO createReview(Review review) {

		// testing the authentication if works keep else remove
		String authenticatedEmail = null;

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername(); // email in your case
		} else {
			authenticatedEmail = principal.toString(); // fallback
		}
		Optional<User> userDetails = userRepository.findById(review.getUser().getId());

		if (userDetails.isEmpty()) {
			throw new ReviewNotFoundException("User not found for review");
		}

		if (!userDetails.get().getEmail().equals(authenticatedEmail)) {
			logger.warn("Access denied: Authenticated user email '{}' does not match order's user email '{}'",
					authenticatedEmail, userDetails.get().getEmail());
			throw new OrderNotFoundException("Access denied: Unauthorized order request");
		}

		logger.info("Access granted for user: {}", authenticatedEmail);
		// auth testing
		// checking whether the user has bought the product and added the review
		boolean hasUserPurchasedProduct  = reviewRepository.hasUserPurchasedProduct(userDetails.get().getId(), review.getProduct().getId());

		if(!hasUserPurchasedProduct){
			throw new ReviewNotFoundException("User did not purchase the product");
		}

		boolean hasUserReviewedProduct  = reviewRepository.hasUserReviewedProduct(userDetails.get().getId(), review.getProduct().getId());

		if(hasUserReviewedProduct){
			throw new ReviewNotFoundException("You already reviewed the product. You can update");
		}

		review.setReviewDate(java.time.LocalDateTime.now()); // FRONTEND: server-side timestamp
		reviewRepository.save(review);

		Optional<Product> productDetails = productRepository.findById(review.getProduct().getId());

		return new ReviewDTO(
				review.getId(),
				new UserDTO(userDetails.get().getUsername(), userDetails.get().getEmail()),
				new ProductReviewDTO(productDetails.get().getName(), productDetails.get().getDescription()),
				review.getRating(),
				review.getComment(),
				review.getReviewDate()
		);
	}
	public String deleteReviewByProductId(Long productId, Long userId) {
		// 🔐 Get authenticated email
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// ✅ Fetch user
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new ReviewNotFoundException("User not found");
		}

		User user = userOpt.get();
		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new ReviewNotFoundException("Access denied: You can only delete your own review");
		}

		// ✅ Find review by user + product
		Optional<Review> reviewOpt = reviewRepository.findByUserIdAndProductId(userId, productId);
		if (reviewOpt.isEmpty()) {
			throw new ReviewNotFoundException("No review found for this product by this user");
		}

		reviewRepository.deleteById(reviewOpt.get().getId());
		return "Deleted Review Successfully for product ID: " + productId;
	}


	public Optional<ReviewDTO> updateReviewByProductId(Long productId, Long userId, String comment, int rating) {

		// 🔐 Get authenticated user's email
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// ✅ Verify user
		Optional<User> userOptional = userRepository.findById(userId);
		if (userOptional.isEmpty()) {
			throw new ReviewNotFoundException("User not found");
		}

		User user = userOptional.get();
		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new ReviewNotFoundException("Access denied: You can only update your own review");
		}

		// ✅ Find review by product ID + user ID
		Optional<Review> reviewOptional = reviewRepository.findByUserIdAndProductId(userId, productId);
		if (reviewOptional.isEmpty()) {
			throw new ReviewNotFoundException("No review found for this product by this user");
		}

		Review review = reviewOptional.get();

		if (comment != null && !comment.trim().isEmpty()) {
			review.setComment(comment);
		}

		if (rating >= 0 && rating <= 5) {
			review.setRating(rating);
		} else {
			throw new ReviewNotFoundException("Rating must be between 0 and 5");
		}

		reviewRepository.save(review);

		Product product = review.getProduct();
		ProductReviewDTO productDTO = new ProductReviewDTO(product.getName(), product.getDescription());
		UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail());

		ReviewDTO reviewDTO = new ReviewDTO(
				review.getId(),
				userDTO,
				productDTO,
				review.getRating(),
				review.getComment(),
				review.getReviewDate()
		);

		return Optional.of(reviewDTO);
	}


	public Optional<ReviewDTO> updateReview(Long id, String status, Long userid, int rating) {


		Optional<Review> reviewOptional = reviewRepository.findById(id);
		if(reviewOptional.isEmpty()) {
			throw new ReviewNotFoundException("Review not found");
		}

		// testing the authentication if works keep else remove
		String authenticatedEmail = null;

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername(); // email in your case
		} else {
			authenticatedEmail = principal.toString(); // fallback
		}
		Optional<User> userDetails = userRepository.findById(userid);

		if (userDetails.isEmpty()) {
			throw new OrderNotFoundException("User not found");
		}

		if (!userDetails.get().getEmail().equals(authenticatedEmail)) {
			logger.warn("Access denied: Authenticated user email '{}' does not match order's user email '{}'",
					authenticatedEmail, userDetails.get().getEmail());
			throw new OrderNotFoundException("Access denied: Unauthorized order request");
		}

		logger.info("Access granted for user: {}", authenticatedEmail);
		// auth testing


		Review review = reviewOptional.get();
		if(status != null){
			review.setComment(status);
		}
		if(rating >= 0 && rating <=5){
			review.setRating(rating);
		} else{
			throw new ReviewNotFoundException("Rating must be between 0 to 5 only");
		}

		reviewRepository.save(review);
		return reviewOptional.map(
				rev -> {
					User user = rev.getUser();
					UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail());

					Product product = rev.getProduct();
					ProductReviewDTO productDTO = new ProductReviewDTO(product.getName(), product.getDescription());
					return new ReviewDTO(rev.getId(), userDTO, productDTO, rating, rev.getComment(), rev.getReviewDate());
				});


    }
    
}
