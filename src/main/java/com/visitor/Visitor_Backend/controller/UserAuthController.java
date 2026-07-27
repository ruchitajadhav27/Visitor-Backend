package com.visitor.Visitor_Backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(originPatterns = {
        "http://localhost:5173",
        "https://visitor-dun.vercel.app",
        "https://*.vercel.app"
})
public class UserAuthController {

    @Value("${brevo.api.key}")
    private String BREVO_API_KEY;

    @Value("${brevo.sender.email}")
    private String SENDER_EMAIL;

    private final Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> requestData) {

        String email = requestData.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Email is required"
                    ));
        }

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        try {

            System.out.println(
                    "========= SENDING VIA BREVO ========="
            );

            RestTemplate restTemplate =
                    new RestTemplate();

            String url =
                    "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.set(
                    "api-key",
                    BREVO_API_KEY
            );

            headers.set(
                    "accept",
                    "application/json"
            );

            Map<String, Object> body =
                    new HashMap<>();

            body.put(
                    "sender",
                    Map.of(
                            "name", "Nexus",
                            "email", SENDER_EMAIL
                    )
            );

            body.put(
                    "to",
                    new Object[]{
                            Map.of(
                                    "email",
                                    email
                            )
                    }
            );

            body.put(
                    "subject",
                    "Login OTP"
            );

            body.put(
                    "htmlContent",
                    "<html><body>"
                            + "<h2>Your OTP is: "
                            + otp
                            + "</h2>"
                            + "<p>Valid for login.</p>"
                            + "</body></html>"
            );

            HttpEntity<Map<String, Object>>
                    entity =
                    new HttpEntity<>(
                            body,
                            headers
                    );

            ResponseEntity<String>
                    response =
                    restTemplate.postForEntity(
                            url,
                            entity,
                            String.class
                    );

            System.out.println(
                    "BREVO SUCCESS RESPONSE: "
                            + response.getBody()
            );

            otpStorage.put(
                    email,
                    otp
            );

            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true
                    )
            );

        } catch (
                HttpClientErrorException e
        ) {

            System.out.println(
                    "--- BREVO ERROR DETAILS ---"
            );

            System.out.println(
                    "Status Code: "
                            + e.getStatusCode()
            );

            System.out.println(
                    "Error Body: "
                            + e.getResponseBodyAsString()
            );

            return ResponseEntity
                    .status(
                            e.getStatusCode()
                    )
                    .body(
                            e.getResponseBodyAsString()
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(500)
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody
            Map<String, String> request
    ) {

        String email =
                request.get("email");

        String otp =
                request.get("otp");

        if (
                otpStorage.containsKey(email)
                        &&
                        otpStorage
                                .get(email)
                                .equals(otp)
        ) {

            otpStorage.remove(email);

            return ResponseEntity.ok(
                    Map.of(
                            "success",
                            true
                    )
            );
        }

        return ResponseEntity
                .status(401)
                .body(
                        Map.of(
                                "success",
                                false,
                                "message",
                                "Invalid OTP"
                        )
                );
    }
}