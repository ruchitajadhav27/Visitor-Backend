package com.visitor.Visitor_Backend.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.visitor.Visitor_Backend.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    
    // ✅ CHANGED: OrderByReceivedAtDesc ko badal kar OrderByIdDesc kiya taaki RECENT top par aaye
    List<Notification> findByRecipientOrderByIdDesc(String recipient);
    
    // For the "Clear All" logic
    List<Notification> findByRecipient(String recipient);
    
    // For counting or checking unread notifications
    List<Notification> findByRecipientAndIsRead(String recipient, boolean isRead);
}