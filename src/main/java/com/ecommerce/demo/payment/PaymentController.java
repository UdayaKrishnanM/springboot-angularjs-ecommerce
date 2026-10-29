package com.ecommerce.demo.payment;

// CHANGE 7: Payment endpoints for the Razorpay integration.

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Step 1: Initiate payment — call this when user clicks "Pay Now"
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestParam Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    // Step 2: Verify & confirm — called after Razorpay JS SDK returns success
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(
            @RequestParam Long orderId,
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam String signature) {
        return ResponseEntity.ok(
                paymentService.confirmPayment(orderId, razorpayOrderId, razorpayPaymentId, signature)
        );
    }
}
