package com.visitor.Visitor_Backend.controller; // Ensure this matches your package name exactly

import com.visitor.Visitor_Backend.model.Admin;
import com.visitor.Visitor_Backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Optional<Admin> admin = adminRepository.findByUsername(username);

        if (admin.isPresent() && admin.get().getPassword().equals(password)) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Login successful"));
        }
        
        return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Invalid Username or Password"));
    }
} 