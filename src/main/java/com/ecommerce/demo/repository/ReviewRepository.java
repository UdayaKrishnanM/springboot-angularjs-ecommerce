package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.user.id = :userId AND oi.product.id = :productId")
    boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT COUNT(r) > 0 FROM Review r WHERE r.user.id = :userId AND r.product.id = :productId")
    boolean hasUserReviewedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findAllByProductId(@Param("productId") Long productId);


    List<Review> findByUserId(Long id);


}
