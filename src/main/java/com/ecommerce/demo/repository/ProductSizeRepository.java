package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.model.ProductSize;
import com.ecommerce.demo.model.SizeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    @Query("select ps from ProductSize ps where ps.product.id = :productId and ps.size = :size")
    Optional<ProductSize> findByProductIdAndSize(@Param("productId") Long productId, @Param("size") SizeType size);

}
