package com.ecommerce.demo.email.services;

// CHANGE 8: Email service upgraded with order notification methods.
// Also fixed: was hardcoded to send to the developer's own email (udayakrishnan2001@gmail.com).
// Now sends to the actual recipient (mailBody.to()).

import com.ecommerce.demo.email.dto.MailBody;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class EmailServices {

    @Autowired
    public JavaMailSenderImpl javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleMessage(MailBody mailBody) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailBody.to());       // FIXED: was hardcoded to developer's email
            message.setFrom(fromEmail);          // FIXED: reads from config not hardcoded
            message.setSubject(mailBody.subject());
            message.setText(mailBody.text());
            javaMailSender.send(message);
        } catch (MailException e) {
            LoggerFactory.getLogger(getClass()).error("Error sending email to {}: {}", mailBody.to(), e.getMessage());
        }
    }

    // CHANGE 8: Order placed notification
    public void sendOrderConfirmation(String toEmail, String username, Long orderId, double totalAmount) {
        String subject = "Order Confirmed! #" + orderId;
        String text = "Hi " + username + ",\n\n"
                + "Your order #" + orderId + " has been placed successfully!\n"
                + "Total Amount: ₹" + String.format("%.2f", totalAmount) + "\n\n"
                + "We'll notify you when your order is shipped.\n\n"
                + "Thank you for shopping with us!";
        sendSimpleMessage(MailBody.builder().to(toEmail).subject(subject).text(text).build());
    }

    // CHANGE 8: Order shipped notification
    public void sendOrderShippedNotification(String toEmail, String username, Long orderId) {
        String subject = "Your order #" + orderId + " has been shipped!";
        String text = "Hi " + username + ",\n\n"
                + "Great news! Your order #" + orderId + " is on its way.\n"
                + "You can track your order from your account.\n\n"
                + "Thank you for your patience!";
        sendSimpleMessage(MailBody.builder().to(toEmail).subject(subject).text(text).build());
    }

    // CHANGE 8: Order delivered notification
    public void sendOrderDeliveredNotification(String toEmail, String username, Long orderId) {
        String subject = "Order #" + orderId + " Delivered! Tell us what you think";
        String text = "Hi " + username + ",\n\n"
                + "Your order #" + orderId + " has been delivered!\n"
                + "We'd love to hear your feedback — leave a review on the app.\n\n"
                + "Thanks for choosing us!";
        sendSimpleMessage(MailBody.builder().to(toEmail).subject(subject).text(text).build());
    }
}
