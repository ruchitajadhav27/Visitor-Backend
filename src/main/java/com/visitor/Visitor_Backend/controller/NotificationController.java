package com.visitor.Visitor_Backend.controller;

import java.util.List;

import com.visitor.Visitor_Backend.repository.SubscriptionTokenRepository;
import com.visitor.Visitor_Backend.model.SubscriptionToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.repository.NotificationRepository;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {
	    "http://localhost:5173",
	    "https://visitor-dun.vercel.app"
	})
public class NotificationController {
    
    @Autowired
    private NotificationRepository repo;
    
    @Autowired
    private SubscriptionTokenRepository tokenRepository;

    // 🌟 STEP 1: WebPushService ko yahan inject karein
    @Autowired
    private com.visitor.Visitor_Backend.service.WebPushService webPushService;
    
    @PostMapping("/save-token")
    public ResponseEntity<?> saveToken(@RequestBody SubscriptionToken newToken) {
        try {
            if (newToken.getEmail() == null || newToken.getEmail().isEmpty()) {
                System.out.println("❌ Save Token Error: Email is empty!");
                return ResponseEntity.badRequest().body("Error: Email cannot be empty!");
            }

            String cleanEmail = newToken.getEmail().toLowerCase().trim();
            newToken.setEmail(cleanEmail);

            System.out.println("📥 Incoming save-token request for: " + cleanEmail);

            List<SubscriptionToken> existingTokens = tokenRepository.findByEmail(cleanEmail);
            if (existingTokens != null && !existingTokens.isEmpty()) {
                System.out.println("🗑️ Cleaning up " + existingTokens.size() + " old tokens for " + cleanEmail);
                tokenRepository.deleteAll(existingTokens);
            }
            
            newToken.setId(null);
            SubscriptionToken saved = tokenRepository.save(newToken);
            System.out.println("✅ Token successfully saved in MongoDB with ID: " + saved.getId());
            
            // 🌟 STEP 2: Token save hote hi automatic test notification trigger karein!
            String jsonPayload = "{\"title\":\"🚀 System Connected!\",\"body\":\"Welcome to Visitor Management System. Push notifications are working!\"}";
            
            System.out.println("📣 Sending instant confirmation push to " + cleanEmail);
            webPushService.sendPushNotification(
                saved.getEndpoint(),
                saved.getP256dh(),
                saved.getAuth(),
                jsonPayload
            );
            
            return ResponseEntity.ok("Token saved and test push sent successfully for " + cleanEmail + "!");
            
        } catch (Exception e) {
            System.err.println("❌ CRITICAL EXCEPTION in saveToken: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage());
        }
    }

    // 1. Admin Notifications Fetch
    @GetMapping("/{recipient}")
    public List<Notification> get(@PathVariable String recipient) {
        // ✅ CHANGED: New repository method called here
        return repo.findByRecipientOrderByIdDesc(recipient);
    }
    
    @PutMapping("/mark-read/{recipient}")
    public void markAsRead(@PathVariable String recipient) {
        List<Notification> unread = repo.findByRecipientAndIsRead(recipient, false);
        unread.forEach(n -> n.setRead(true));
        repo.saveAll(unread);
    }

    @DeleteMapping("/recipient/{recipient}")
    public void deleteByRecipient(@PathVariable String recipient) {
        List<Notification> notifications = repo.findByRecipient(recipient);
        repo.deleteAll(notifications);
    }

    // 2. Fetch User Notifications History
    @GetMapping("/user/{email}")
    public List<Notification> getUserNotifications(@PathVariable String email) {
        String cleanEmail = email.toLowerCase().trim();
        // ✅ CHANGED: New repository method called here too
        return repo.findByRecipientOrderByIdDesc(cleanEmail);
    }

    @PutMapping("/mark-read/user/{email}")
    public void markUserAsRead(@PathVariable String email) {
        String cleanEmail = email.toLowerCase().trim();
        List<Notification> unread = repo.findByRecipientAndIsRead(cleanEmail, false);
        if (!unread.isEmpty()) {
            unread.forEach(n -> n.setRead(true));
            repo.saveAll(unread);
        }
    }

    @DeleteMapping("/user/{email}")
    public void clearUserNotifications(@PathVariable String email) {
        List<Notification> notifications = repo.findByRecipient(email);
        repo.deleteAll(notifications);
    }
 
    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable String id) {
        repo.deleteById(id);
    }
}