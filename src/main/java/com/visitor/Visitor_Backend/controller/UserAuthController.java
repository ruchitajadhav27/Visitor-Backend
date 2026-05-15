package com.visitor.Visitor_Backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://visitor-dun.vercel.app"
})
public class UserAuthController {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email is required")
            );
        }

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        try {

            System.out.println("========= OTP API HIT =========");
            
            System.out.println("MAIL HOST = " + mailHost);
            System.out.println("MAIL PORT = " + mailPort);
            System.out.println("SENDER = " + senderEmail);
            
            
            System.out.println("MAIL USER = " + senderEmail);
            System.out.println("Sending OTP to = " + email);

            SimpleMailMessage message =
                    new SimpleMailMessage();

            // Use configured email from application.properties
            message.setFrom(senderEmail);

            message.setTo(email);
            message.setSubject("Your Login OTP");
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP for login is: " + otp +
                    "\n\nValid for a short time.\n\n" +
                    "Regards,\nModern Enquiry Team"
            );
            
            System.out.println("TRYING TO SEND MAIL...");

            mailSender.send(message);
            System.out.println("MAIL SENT");

            otpStorage.put(email, otp);

            System.out.println("OTP SENT SUCCESSFULLY");
            System.out.println("OTP for " + email + " = " + otp);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP Sent Successfully"
            ));

        } catch (Exception e) {

            System.out.println("========= OTP MAIL ERROR =========");
            e.printStackTrace();

            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "error", e.toString()
                    )
            );
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        if (otpStorage.containsKey(email)
                && otpStorage.get(email).equals(otp)) {

            otpStorage.remove(email);

            return ResponseEntity.ok(
                    Map.of("success", true)
            );
        }

        return ResponseEntity.status(401).body(
                Map.of(
                        "success", false,
                        "message", "Invalid OTP"
                )
        );
    }
}