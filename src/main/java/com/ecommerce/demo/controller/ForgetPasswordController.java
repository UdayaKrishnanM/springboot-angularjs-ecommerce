package com.ecommerce.demo.controller;

// CHANGE 3: Moved from email/entities/ to controller/ where it belongs.
// A controller has nothing to do with "entities". Package structure should
// reflect what a class IS, not just where you happened to put it.

import com.ecommerce.demo.email.dto.MailBody;
import com.ecommerce.demo.email.entities.ChangePassword;
import com.ecommerce.demo.email.entities.ForgetPassword;
import com.ecommerce.demo.email.entities.ForgetPasswordRepo;
import com.ecommerce.demo.email.services.EmailServices;
import com.ecommerce.demo.model.User;
import com.ecommerce.demo.repository.UserRepository;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/forgetPassword")
public class ForgetPasswordController {

    @Autowired
    public UserRepository userRepository;

    @Autowired
    public EmailServices emailServices;

    @Autowired
    public ForgetPasswordRepo forgetPasswordRepo;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @PostMapping("/verifyEmail/{email}")
    public ResponseEntity<String> sendEmail(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return new ResponseEntity<>("Email id not found", HttpStatus.NOT_FOUND);
        }
        if (user.get().getForgetPassword() != null && user.get().getForgetPassword().getExpirationTime().before(new Date())) {
            forgetPasswordRepo.deleteById(user.get().getForgetPassword().getForgetID());
        }
        if (user.get().getForgetPassword() != null && user.get().getForgetPassword().getExpirationTime().after(new Date())) {
            return new ResponseEntity<>("OTP already generated", HttpStatus.CONFLICT);
        }

        int otp = new Random().nextInt(100_000, 999_999);

        MailBody mailBody = MailBody.builder()
                .to(email)
                .text("This is the OTP for your Forgotten Password: " + otp)
                .subject("OTP for forgotten password")
                .build();

        ForgetPassword fp = ForgetPassword.builder()
                .otp(otp)
                .expirationTime(new Date(System.currentTimeMillis() + 700 * 1000))
                .user(user.get())
                .build();

        emailServices.sendSimpleMessage(mailBody);
        forgetPasswordRepo.save(fp);
        return ResponseEntity.ok("Email sent for verification");
    }

    @PostMapping("/verifyOtp/{otp}/{email}")
    public ResponseEntity<String> verifyOtp(@PathVariable Integer otp, @PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return new ResponseEntity<>("Provide a valid Email", HttpStatus.NOT_FOUND);
        }
        Optional<ForgetPassword> fp = forgetPasswordRepo.findByOtpAndUser(otp, user.get());
        if (fp.isEmpty()) {
            return new ResponseEntity<>("Invalid OTP", HttpStatus.UNAUTHORIZED);
        }
        if (fp.get().getExpirationTime().before(Date.from(Instant.now()))) {
            forgetPasswordRepo.deleteById(fp.get().getForgetID());
            return new ResponseEntity<>("OTP has expired!", HttpStatus.EXPECTATION_FAILED);
        }
        return ResponseEntity.ok("OTP is verified!");
    }

    @PostMapping("/changePassword/{email}/{otp}")
    public ResponseEntity<String> changePassword(@RequestBody ChangePassword changePassword,
                                                  @PathVariable String email,
                                                  @PathVariable Integer otp) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return new ResponseEntity<>("Provide a valid Email", HttpStatus.NOT_FOUND);
        }
        Optional<ForgetPassword> fp = forgetPasswordRepo.findByOtpAndUser(otp, user.get());
        if (fp.isEmpty()) {
            return new ResponseEntity<>("OTP not found! Please generate a new OTP.", HttpStatus.NOT_FOUND);
        }
        if (fp.get().getExpirationTime().before(Date.from(Instant.now()))) {
            forgetPasswordRepo.deleteById(fp.get().getForgetID());
            return new ResponseEntity<>("OTP has expired! Please request a new one.", HttpStatus.EXPECTATION_FAILED);
        }
        if (!Objects.equals(changePassword.password(), changePassword.repeatPassword())) {
            return new ResponseEntity<>("Passwords do not match!", HttpStatus.EXPECTATION_FAILED);
        }
        String encodedPassword = passwordEncoder.encode(changePassword.password());
        userRepository.updatePassword(email, encodedPassword);
        forgetPasswordRepo.deleteById(fp.get().getForgetID());
        return ResponseEntity.ok("Password has been changed successfully!");
    }
}
