package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.LoginDevice;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginDeviceRepository extends JpaRepository<LoginDevice, Long> {

    List<LoginDevice> findByUserId(Long userId);


    @Query("SELECT d FROM LoginDevice d WHERE d.user.email = :email")
    List<LoginDevice> findByUserEmail(@Param("email") String email);


    Optional<LoginDevice> findByDeviceIdAndUserId(String deviceId, Long userId);


    @Query("SELECT ld FROM LoginDevice ld WHERE ld.deviceId = :deviceId AND ld.user.email = :emailId")
    Optional<LoginDevice> findByDeviceIdAndEmailId(@Param("deviceId") String deviceId, @Param("emailId") String emailId);


}
