package com.visitor.Visitor_Backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://visitor-dun.vercel.app"
})
public class UserAuthController {

    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @org.springframework.web.bind.annotation.RequestBody
            Map<String, String> requestData) {

        String email = requestData.get("email");

        if (email == null || email.trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Email is required"
                    )
            );
        }

        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        try {

            System.out.println("========= OTP API HIT =========");
            System.out.println("Sending OTP to = " + email);

            OkHttpClient client = new OkHttpClient();

            String json = """
            {
              "from": "onboarding@resend.dev",
              "to": ["%s"],
              "subject": "Your Login OTP",
              "html": "<h2>Your OTP is: %s</h2>"
            }
            """.formatted(email, otp);

            okhttp3.RequestBody body =
                    okhttp3.RequestBody.create(
                            json,
                            MediaType.parse("application/json")
                    );

            Request resendRequest = new Request.Builder()
                    .url("https://api.resend.com/emails")
                    .post(body)
                    .addHeader(
                            "Authorization",
                            "Bearer " + System.getenv("RESEND_API_KEY")
                    )
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response =
                    client.newCall(resendRequest).execute();

            String responseBody = response.body().string();

            System.out.println("========= RESEND RESPONSE =========");
            System.out.println(responseBody);

            otpStorage.put(email, otp);

            System.out.println("OTP SENT SUCCESSFULLY");
            System.out.println("OTP for " + email + " = " + otp);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "OTP Sent Successfully"
                    )
            );

        } catch (Exception e) {

            System.out.println("========= OTP MAIL ERROR =========");

            e.printStackTrace();

            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage(),
                            "fullError", e.toString()
                    )
            );
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @org.springframework.web.bind.annotation.RequestBody
            Map<String, String> requestData) {

        String email = requestData.get("email");
        String otp = requestData.get("otp");

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