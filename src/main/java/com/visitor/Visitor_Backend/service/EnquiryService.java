package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.Enquiry;
import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.repository.EnquiryRepository;
import com.visitor.Visitor_Backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class EnquiryService {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void processEnquiry(Enquiry enquiry) {
        // 1. Save Enquiry to Database
        enquiryRepository.save(enquiry);

        // 2. Send Email to Admin
        sendEmail(enquiry);

        // 3. Create and Save Notification for Admin Dashboard
        Notification notification = new Notification();
        
        // IMPORTANT: Must be "ADMIN" so Header.jsx can fetch it
        notification.setRecipient("ADMIN"); 
        notification.setMessage("New Enquiry from: " + enquiry.getFirstName() + " " + enquiry.getLastName() + 
                " - Message: " + enquiry.getMessage());
        notification.setPurpose("Visitor Inquiry");
        notification.setStatus("New");
        
        // IMPORTANT: Ensure your Model uses 'isRead' to match frontend '!n.isRead'
        notification.setRead(false); 
        
        // FIXED THE TYPO HERE: Changed eTimeFormatter to DateTimeFormatter
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        notification.setDateTime(LocalDateTime.now().format(dtFormatter));
        
        notification.setReceivedAt(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")));

        // 4. Save and Push
        Notification savedNotification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/admin-notifications", savedNotification);
    }

    private void sendEmail(Enquiry enquiry) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo("jadhavruchita27@gmail.com"); // Replace with your email
            mail.setReplyTo(enquiry.getEmail());
            mail.setSubject("VIMS: New Contact Message");
            mail.setText("You have received a new message from the visitors contact form.\n\n" +
                         "Name: " + enquiry.getFirstName() + " " + enquiry.getLastName() + "\n" +
                         "Mobile: " + enquiry.getMobile() + "\n" +
                         "Email: " + enquiry.getEmail() + "\n" +
                         "Message: " + enquiry.getMessage() + "\n\n" +
                         "Submitted at: " + enquiry.getSubmittedAt());
            
            mailSender.send(mail);
        } catch (Exception e) {
            System.err.println("Email failed to send: " + e.getMessage());
            // We don't block the process if email fails
        }
    }
}