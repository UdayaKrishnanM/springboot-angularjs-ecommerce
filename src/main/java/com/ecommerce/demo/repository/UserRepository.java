package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.User;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);


    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    
    @Modifying
    @Transactional
    @Query("update User u SET u.password = ?2 WHERE u.email = ?1")
    void updatePassword(String email, String password);
    
}
