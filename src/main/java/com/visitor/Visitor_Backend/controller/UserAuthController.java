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

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        // Store OTP
        otpStorage.put(email, otp);

        // PRINT OTP IN LOGS
        System.out.println("=================================");
        System.out.println("OTP for " + email + " = " + otp);
        System.out.println("=================================");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP Generated",
                "otp", otp   // optional
        ));
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