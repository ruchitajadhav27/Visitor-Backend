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
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));

        // Brevo API URL
        String url = "https://api.brevo.com/v3/smtp/email";

        // Prepare the JSON body for HTTP API
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", "Modern Enquiry", "email", senderEmail));
        body.put("to", List.of(Map.of("email", email)));
        body.put("subject", "Your Login OTP");
        body.put("textContent", "Your OTP is: " + otp);

        // Set Headers (This is the key part!)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey); 

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            System.out.println("Sending OTP via HTTP API to: " + email);
            
            // Sending the actual HTTP POST request
            restTemplate.postForEntity(url, entity, String.class);
            
            otpStorage.put(email, otp);
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP Sent via HTTP API"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "API Error: " + e.getMessage()));
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