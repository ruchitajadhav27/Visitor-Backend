package com.visitor.Visitor_Backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.visitor.Visitor_Backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    // For fetching the list
    List<Notification> findByRecipientOrderByReceivedAtDesc(String recipient);
    
    // For the "Clear All" logic
    List<Notification> findByRecipient(String recipient);
    
    List<Notification> findByRecipientAndIsRead(String recipient, boolean isRead);
}
