package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.Product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
	

	Optional<Product> findByName(String name);

	Product save(Product product);

	
	
}
