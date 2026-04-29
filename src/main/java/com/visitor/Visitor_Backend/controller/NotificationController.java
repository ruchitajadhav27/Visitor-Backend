package com.visitor.Visitor_Backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.repository.NotificationRepository;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173") // Allow your React app
public class NotificationController {
    @Autowired
    private NotificationRepository repo;

    @GetMapping("/{recipient}")
    public List<Notification> get(@PathVariable String recipient) {
        return repo.findByRecipientOrderByReceivedAtDesc(recipient);
    }
    
 // --- ADD THIS NEW METHOD ---
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


 // 1. Fetch user history
    @GetMapping("/user/{email}")
    public List<Notification> getUserNotifications(@PathVariable String email) {
        // Ensure email is cleaned to match how it's stored
        String cleanEmail = email.toLowerCase().trim();
        return repo.findByRecipientOrderByReceivedAtDesc(cleanEmail);
    }

 // 2. Mark all as read for user
    @PutMapping("/mark-read/user/{email}")
    public void markUserAsRead(@PathVariable String email) {
        String cleanEmail = email.toLowerCase().trim();
        List<Notification> unread = repo.findByRecipientAndIsRead(cleanEmail, false);
        if (!unread.isEmpty()) {
            unread.forEach(n -> n.setRead(true));
            repo.saveAll(unread);
        }
    }

 // 3. Delete all for user
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
