package com.visitor.Visitor_Backend.controller;


import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {
	    "http://localhost:5173",
	    "https://visitor-dun.vercel.app"
	})
public class UserAuthController {

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email"); 
        String otp = String.valueOf((int)((Math.random() * 900000) + 100000));
        
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your Login OTP");
            message.setText("Your Modern Enquiry login code is: " + otp);
            mailSender.send(message);

            otpStorage.put(email, otp);
            return ResponseEntity.ok(Map.of("message", "OTP Sent Successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
            otpStorage.remove(email);
            return ResponseEntity.ok(Map.of("success", true));
        }
        return ResponseEntity.status(401).body(Map.of("success", false));
    }
}