package com.ecommerce.demo.payment;

// CHANGE 7: Razorpay payment integration.
//
// Flow:
//   1. User calls POST /api/user/payment/create?orderId=5
//      → Backend creates a Razorpay order, returns razorpayOrderId + amount
//   2. Frontend collects card/UPI details using Razorpay's JS SDK
//      → On success, Razorpay returns a paymentId + signature
//   3. User calls POST /api/user/payment/verify
//      → Backend verifies the HMAC signature to confirm payment wasn't tampered
//      → If valid, marks the order as PAID
//
// This is a realistic payment flow used in production apps.
// Even with test keys, an interviewer can see the architecture is correct.
//
// NOTE: Add razorpay-java to pom.xml to use the real SDK.
//       The stub below compiles without it for environments that don't need it.

import com.ecommerce.demo.exception.OrderNotFoundException;
import com.ecommerce.demo.model.Order;
import com.ecommerce.demo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Step 1: Initiate payment for an existing order.
     * Creates a Razorpay order and returns the details needed by the frontend.
     */
    public Map<String, Object> initiatePayment(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        Order order = orderOpt.get();

        // In production: use Razorpay SDK to create the order
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject options = new JSONObject();
        // options.put("amount", (int)(order.getTotalAmount() * 100)); // paise
        // options.put("currency", "INR");
        // options.put("receipt", "order_" + orderId);
        // com.razorpay.Order razorpayOrder = client.orders.create(options);

        // Stub response for demo/testing:
        Map<String, Object> response = new HashMap<>();
        response.put("razorpayOrderId", "order_stub_" + orderId);
        response.put("amount", (int) (order.getTotalAmount() * 100)); // in paise
        response.put("currency", "INR");
        response.put("keyId", keyId);
        response.put("orderId", orderId);

        logger.info("Payment initiated for order {}: amount={}", orderId, order.getTotalAmount());
        return response;
    }

    /**
     * Step 2: Verify Razorpay's payment signature to confirm payment is genuine.
     * Razorpay signs the payment with HMAC-SHA256 using your key secret.
     * We recompute the signature and compare — if they match, payment is authentic.
     */
    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            String expectedSignature = sb.toString();

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            logger.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Step 3: Mark the order as PAID after successful verification.
     */
    public String confirmPayment(Long orderId, String razorpayOrderId, String razorpayPaymentId, String signature) {
        boolean isValid = verifyPayment(razorpayOrderId, razorpayPaymentId, signature);
        if (!isValid) {
            return "Payment verification failed. Invalid signature.";
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        order.setStatus("PAID");
        orderRepository.save(order);
        logger.info("Payment confirmed for order {}. PaymentId: {}", orderId, razorpayPaymentId);
        return "Payment successful! Order " + orderId + " is now PAID.";
    }
}
