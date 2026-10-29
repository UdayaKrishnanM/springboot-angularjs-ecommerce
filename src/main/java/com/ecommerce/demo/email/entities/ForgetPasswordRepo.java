package com.ecommerce.demo.email.entities;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.demo.model.User;


@Repository
public interface ForgetPasswordRepo extends JpaRepository<ForgetPassword, Integer> {

	@Query("select fp from ForgetPassword fp where fp.otp= ?1 and fp.user= ?2")
	Optional<ForgetPassword> findByOtpAndUser(Integer otp, User user);
	
}
