package com.visitor.Visitor_Backend.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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