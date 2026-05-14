package com.visitor.Visitor_Backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:5173", "https://visitor-dun.vercel.app"})
public class UserAuthController {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));

        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            // Prepare Brevo JSON Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("name", "Modern Enquiry Team", "email", senderEmail));
            payload.put("to", List.of(Map.of("email", email)));
            payload.put("subject", "Your Login OTP");
            payload.put("textContent", "Hello, Your OTP for login is: " + otp);

            // Set Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Send via HTTP POST
            restTemplate.postForEntity(url, entity, String.class);

            otpStorage.put(email, otp);
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP Sent via API"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("success", false, "error", "API Error: " + e.getMessage()));
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
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid OTP"));
    }
}