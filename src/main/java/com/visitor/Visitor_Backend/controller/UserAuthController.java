package com.visitor.Visitor_Backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // Thread-safe OTP storage
    private final Map<String, String> otpStorage =
            new ConcurrentHashMap<>();


    // ==============================
    // SEND OTP API
    // ==============================
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");

        // Validate email
        if (email == null || email.trim().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Email is required"
                    )
            );
        }

        // Generate 6-digit OTP
        String otp = String.valueOf(
                (int) ((Math.random() * 900000) + 100000)
        );

        try {

            System.out.println("\n========= OTP API HIT =========");

            System.out.println("MAIL HOST = " + mailHost);
            System.out.println("MAIL PORT = " + mailPort);
            System.out.println("SENDER EMAIL = " + senderEmail);

            System.out.println("Sending OTP to = " + email);

            // Create Mail Message
            SimpleMailMessage message =
                    new SimpleMailMessage();

            // Sender
            message.setFrom(
                    "Modern Enquiry <" + senderEmail + ">"
            );

            // Receiver
            message.setTo(email);

            // Subject
            message.setSubject("Your Login OTP");

            // Body
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP for login is: " + otp +
                    "\n\n" +
                    "This OTP is valid for 5 minutes." +
                    "\n\n" +
                    "Regards,\n" +
                    "Modern Enquiry Team"
            );

            System.out.println("TRYING TO SEND MAIL...");

            // Send Mail
            mailSender.send(message);

            System.out.println("MAIL SENT SUCCESSFULLY");

            // Store OTP
            otpStorage.put(email, otp);

            System.out.println("OTP STORED");
            System.out.println("OTP for " + email + " = " + otp);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "OTP Sent Successfully"
                    )
            );

        } catch (Exception e) {

            System.out.println(
                    "\n========= OTP MAIL ERROR ========="
            );

            e.printStackTrace();

            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "message", "Failed to send OTP",
                            "error", e.getMessage()
                    )
            );
        }
    }


    // ==============================
    // VERIFY OTP API
    // ==============================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String otp = request.get("otp");

        // Validate OTP
        if (otpStorage.containsKey(email)
                && otpStorage.get(email).equals(otp)) {

            // Remove OTP after successful verification
            otpStorage.remove(email);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "OTP Verified Successfully"
                    )
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