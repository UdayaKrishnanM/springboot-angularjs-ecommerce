package com.ecommerce.demo.service;

import java.util.*;
import java.util.stream.Collectors;

import com.ecommerce.demo.exception.UserNameNotFoundException;
import com.ecommerce.demo.model.*;
import com.ecommerce.demo.repository.ProductSizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ecommerce.demo.dto.CartItemResponseDTO;
import com.ecommerce.demo.dto.CartResponseDTO;
import com.ecommerce.demo.dto.ItemDTO;
import com.ecommerce.demo.dto.ProductDTO;
import com.ecommerce.demo.dto.UserDTO;
import com.ecommerce.demo.dto.UserOrderResponse;
import com.ecommerce.demo.exception.CartItemNotFoundException;
import com.ecommerce.demo.exception.ProductNotFoundException;
import com.ecommerce.demo.repository.CartItemRepository;
import com.ecommerce.demo.repository.ProductRepository;
import com.ecommerce.demo.repository.UserRepository;

@Service
public class CartItemService {
	
	private static final Logger logger = LoggerFactory.getLogger(CartItemService.class);

	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductSizeRepository productSizeRepository;
	
	@Autowired
	private CartItemRepository cartItemRepository;
	
	public List<CartItem> getCartItemsByUserId(Long userId){
		return cartItemRepository.findByUserId(userId);
	}

	public String addCartItem(CartItem cartItem) {
		// 🔐 Authenticated user's email
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// ✅ Validate user
		Optional<User> userOptional = userRepository.findById(cartItem.getUser().getId());
		if (userOptional.isEmpty()) {
			return "User not found";
		}

		User user = userOptional.get();
		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new UserNameNotFoundException("Access denied: You can only modify your own cart");
		}

		// ✅ Validate product
		Optional<Product> productOptional = productRepository.findById(cartItem.getProduct().getId());
		if (productOptional.isEmpty()) {
			return "Product not found";
		}

		Product product = productOptional.get();

		// ✅ Validate size input
		if (cartItem.getProductSize() == null || cartItem.getProductSize().getSize() == null) {
			return "Size must be provided";
		}

		Optional<ProductSize> productSizeOptional =
				productSizeRepository.findByProductIdAndSize(product.getId(), cartItem.getProductSize().getSize());

		if (productSizeOptional.isEmpty()) {
			return "Selected size is not available for this product";
		}

		ProductSize selectedSize = productSizeOptional.get();

		// ✅ Ensure quantity is at least 1
		if (cartItem.getQuantity() <= 0) {
			cartItem.setQuantity(1);
		}

		// 🔎 Check if this product is already in the cart for the user
		List<CartItem> existingCartItems = cartItemRepository.findByUserId(user.getId());

		Optional<CartItem> matchingItemOptional = existingCartItems.stream()
				.filter(item -> item.getProduct().getId().equals(product.getId()))
				.findFirst();

		if (matchingItemOptional.isPresent()) {
			CartItem existingItem = matchingItemOptional.get();

			if (existingItem.getProductSize().getSize() == selectedSize.getSize()) {
				// ✅ Same size → merge quantity
				existingItem.setQuantity(existingItem.getQuantity() + cartItem.getQuantity());
				cartItemRepository.save(existingItem);
				return "Cart updated: Quantity merged for same size.";
			} else {
				// 🔁 Different size → update existing record with new size and quantity
				existingItem.setProductSize(selectedSize);
//				existingItem.setProductSize(selectedSize); // update enum field too, if used
				existingItem.setQuantity(cartItem.getQuantity());
				cartItemRepository.save(existingItem);
				return "Cart updated: Size changed and quantity updated.";
			}
		}

		// ➕ If product not in cart → create new cart item
		cartItem.setProductSize(selectedSize); // attach managed entity
		cartItemRepository.save(cartItem);

		return "Item added to cart successfully!";
	}


	public Optional<CartResponseDTO> updateCartItem(CartItem updateCartItem) {
		// 🔐 Get authenticated user
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// ✅ Validate user
		Optional<User> userOpt = userRepository.findById(updateCartItem.getUser().getId());
		if (userOpt.isEmpty()) {
			throw new CartItemNotFoundException("User not found");
		}

		User user = userOpt.get();
		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new CartItemNotFoundException("Access denied: Unauthorized user");
		}

		// ✅ Validate product
		Long productId = updateCartItem.getProduct().getId();
		Optional<Product> productOpt = productRepository.findById(productId);
		if (productOpt.isEmpty()) {
			throw new CartItemNotFoundException("Product not found");
		}

		Product product = productOpt.get();

		// ✅ Validate size
		SizeType requestedSize = updateCartItem.getProductSize().getSize();
		Optional<ProductSize> sizeOpt = productSizeRepository.findByProductIdAndSize(productId, requestedSize);
		if (sizeOpt.isEmpty()) {
			throw new CartItemNotFoundException("Size not available for this product");
		}

		ProductSize productSize = sizeOpt.get();

		if (updateCartItem.getQuantity() > productSize.getStockQuantity()) {
			throw new CartItemNotFoundException("Not enough stock for size: " + requestedSize);
		}

		// ✅ Check for same product and size
		Optional<CartItem> sameSizeItemOpt = cartItemRepository.findByUserIdAndProductIdAndProductSizeId(
				user.getId(), productId, productSize.getId());

		if (sameSizeItemOpt.isPresent()) {
			CartItem existingItem = sameSizeItemOpt.get();
			int newQuantity = existingItem.getQuantity() + updateCartItem.getQuantity();
			existingItem.setQuantity(newQuantity);
			cartItemRepository.save(existingItem);

			return Optional.of(buildCartResponse(user, existingItem));
		} else {
			// 🔍 Check if product exists in cart with different size
			List<CartItem> userCartItems = cartItemRepository.findByUserId(user.getId());

			for (CartItem item : userCartItems) {
				if (item.getProduct().getId().equals(productId)) {
					// ✅ Update that item
					item.setProductSize(productSize);
					item.setQuantity(updateCartItem.getQuantity());
					cartItemRepository.save(item);

					return Optional.of(buildCartResponse(user, item));
				}
			}
		}

		// ❌ No matching item found → do NOT create new
		throw new CartItemNotFoundException("No matching cart item found to update.");
	}

	private CartResponseDTO buildCartResponse(User user, CartItem cartItem) {
		UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail());

		Product product = cartItem.getProduct();
		ProductDTO productDTO = new ProductDTO(
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getCategory(),
				cartItem.getProductSize().getSize().toString()
		);

		CartItemResponseDTO itemDTO = new CartItemResponseDTO(productDTO, cartItem.getQuantity());

		CartResponseDTO responseDTO = new CartResponseDTO();
		responseDTO.setUser(userDTO);
		responseDTO.setCartItems(List.of(itemDTO));

		return responseDTO;
	}




	public CartResponseDTO getCartItemByUserId() {

		// 🔐 Get authenticated user's email
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// ✅ Validate user
		Optional<User> userOptional = userRepository.findByEmail(authenticatedEmail);
		if (userOptional.isEmpty()) {
			throw new UserNameNotFoundException("User not found");
		}

		User user = userOptional.get();

		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new CartItemNotFoundException("Access denied");
		}

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());

		UserDTO userDTO = new UserDTO(user.getUsername(), user.getEmail());

		List<CartItemResponseDTO> cartItemDTOs = cartItems.stream().map(cartItem -> {
			CartItemResponseDTO dto = new CartItemResponseDTO();

			Product cartItemProduct = cartItem.getProduct();
			SizeType selectedSize = cartItem.getProductSize().getSize();

			ProductDTO productDTO = new ProductDTO(
					cartItemProduct.getName(),
					cartItemProduct.getDescription(),
					cartItemProduct.getPrice(),
					cartItemProduct.getCategory(),
					selectedSize.toString()
			);

			productDTO.setId(cartItemProduct.getId());
			dto.setProduct(productDTO);
			dto.setQuantity(cartItem.getQuantity());
			dto.setId(cartItem.getId());               // FRONTEND
			dto.setProductId(cartItemProduct.getId()); // FRONTEND
			return dto;
		}).collect(Collectors.toList());

        CartResponseDTO responseDTO = new CartResponseDTO();
        responseDTO.setUser(userDTO);
        responseDTO.setCartItems(cartItemDTOs);

        return responseDTO;
    }

	public UserOrderResponse getUserOrders(Long userId) {
		
		Optional<User> user  = userRepository.findById(userId);
		
		if(user.isEmpty()) {
			throw new CartItemNotFoundException("User ID : " + userId + " not found");
		} else {

			List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
		
			UserDTO userDTO = new UserDTO(user.get().getUsername(), user.get().getEmail());
			List<ItemDTO> itemDTOs = cartItems.stream()
					.map(cartItem -> new ItemDTO(
							new ProductDTO(cartItem.getProduct().getName(), cartItem.getProduct().getDescription(),
									cartItem.getProduct().getPrice(), cartItem.getProduct().getCategory()),
									cartItem.getQuantity()
									)
							).collect(Collectors.toList());
					
			return new UserOrderResponse(userDTO, itemDTOs);
		}		
	}
	
	@ExceptionHandler(CartItemNotFoundException.class)
	public ResponseEntity<String> handleOrderNotFoundException(CartItemNotFoundException ex) {
		return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	public String deleteCartItem(Long id, CartItem cartItemUser) {
		// 🔐 1. Get authenticated user's email
		String authenticatedEmail;
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserDetails) {
			authenticatedEmail = ((UserDetails) principal).getUsername();
		} else {
			authenticatedEmail = principal.toString();
		}

		// 🔐 2. Fetch user from cartItemUser and validate auth match
		Optional<User> userOpt = userRepository.findById(cartItemUser.getUser().getId());
		if (userOpt.isEmpty()) {
			throw new CartItemNotFoundException("User not found");
		}

		User user = userOpt.get();
		if (!user.getEmail().equals(authenticatedEmail)) {
			throw new CartItemNotFoundException("Access denied: User authentication failed");
		}

		// ✅ 3. Fetch the cart item
		Optional<CartItem> cartItem = cartItemRepository.findById(id);
		if (cartItem.isEmpty()) {
			throw new CartItemNotFoundException("Cart item ID not found: " + id);
		}

		// ✅ 4. Check if the cart item belongs to the authenticated user
		if (!cartItem.get().getUser().getId().equals(user.getId())) {
			throw new CartItemNotFoundException("Access denied: Cart item doesn't belong to you");
		}

		// ✅ 5. Delete the cart item
		cartItemRepository.deleteById(id);
		return "Deleted successfully";
	}




}
