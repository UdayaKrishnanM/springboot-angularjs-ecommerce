package com.ecommerce.demo.repository;

import java.util.List;
import java.util.Optional;

import com.ecommerce.demo.model.SizeType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.demo.model.CartItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long>{

	@Query("SELECT ci FROM CartItem ci WHERE ci.user.id = :userId AND ci.product.id = :productId AND ci.productSize.id = :sizeId")
	Optional<CartItem> findByUserIdAndProductIdAndProductSizeId(@Param("userId") Long userId,
																@Param("productId") Long productId,
																@Param("sizeId") Long sizeId);


	List<CartItem> findByUserId(Long userId);
	
}
