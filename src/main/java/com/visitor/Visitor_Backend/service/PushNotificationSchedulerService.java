package com.visitor.Visitor_Backend.service;

import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.model.SubscriptionToken;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import com.visitor.Visitor_Backend.repository.SubscriptionTokenRepository;

@Service
public class PushNotificationSchedulerService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SubscriptionTokenRepository tokenRepository;

    @Autowired
    private WebPushService webPushService;

    // Har minute par check karega
    @Scheduled(cron = "0 * * * * ?")
    public void checkTimeOutMeetings() {
        
        LocalTime now = LocalTime.now();
        String timeUpper = now.format(DateTimeFormatter.ofPattern("hh:mm a")); // Ex: "04:44 PM"
        String timeLower = timeUpper.toLowerCase();                           // Ex: "04:44 pm"
        String time24 = now.format(DateTimeFormatter.ofPattern("HH:mm"));      // Ex: "16:44"
        
        System.out.println("\n=== ⏰ SCHEDULER START ===");
        System.out.println("🔍 Current System Time Formats to Match:");
        System.out.println("   👉 Upper: [" + timeUpper + "]");
        System.out.println("   👉 Lower: [" + timeLower + "]");
        System.out.println("   👉 24-Hr: [" + time24 + "]");

        // 🌟 DIAGNOSTIC check: Database mein total active entries hain bhi ya nahi?
        long totalApproved = appointmentRepository.count(); // Isko badal kar findByStatus query bhi dekh sakte hain
        System.out.println("📊 Total appointments currently present in collection: " + totalApproved);

        // Sabhi possible formats se fetch karne ka try karein
        List<Appointment> expiringMeetings = appointmentRepository.findByStatusAndTimeOut("Approved", timeUpper);
        if (expiringMeetings.isEmpty()) {
            expiringMeetings = appointmentRepository.findByStatusAndTimeOut("Approved", timeLower);
        }
        if (expiringMeetings.isEmpty()) {
            expiringMeetings = appointmentRepository.findByStatusAndTimeOut("Approved", time24);
        }
        
        // Agar status case-sensitive issue hai toh bina status ke bhi match karke dekh sakte hain debug ke liye
        if (expiringMeetings.isEmpty()) {
            System.out.println("ℹ️ No scheduled matches found for targeted time fields.");
            System.out.println("=== ⏰ SCHEDULER END ===\n");
            return;
        }

        System.out.println("🎯 MATCH FOUND! Total expiring meeting(s): " + expiringMeetings.size());

        for (Appointment app : expiringMeetings) {
            String userEmail = app.getEmail(); 

            if (userEmail == null || userEmail.isEmpty()) {
                System.out.println("⚠️ Skipped: No email found mapped for visitor: " + app.getVisitorName());
                continue;
            }

            String cleanEmail = userEmail.toLowerCase().trim();
            System.out.println("🔍 Fetching subscription tokens from MongoDB for: " + cleanEmail);
            
            List<SubscriptionToken> tokens = tokenRepository.findByEmail(cleanEmail);
            
            if (tokens == null || tokens.isEmpty()) {
                System.out.println("⚠️ Subscriptions Empty: Token collection has NO keys for " + cleanEmail);
                continue;
            }

            String safeVisitorName = app.getVisitorName() != null ? app.getVisitorName().replace("\"", "\\\"") : "Visitor";

            String jsonPayload = String.format(
                "{\"title\":\"⏰ TIME IS UP!\",\"body\":\"Your meeting time has ended, %s.\"}", 
                safeVisitorName
            );

            System.out.println("📦 Encrypted strict JSON Payload: " + jsonPayload);
            
            for (SubscriptionToken token : tokens) {
                try {
                    System.out.println("📤 Outbound push streaming to: " + token.getEndpoint().substring(0, Math.min(token.getEndpoint().length(), 40)) + "...");
                    
                    webPushService.sendPushNotification(
                        token.getEndpoint(), 
                        token.getP256dh(), 
                        token.getAuth(), 
                        jsonPayload
                    );
                    
                    System.out.println("🚀 Desktop Push Delivered successfully to endpoint server!");
                } catch (Exception e) {
                    System.err.println("❌ Delivery crashed for specific client stream: " + e.getMessage());
                }
            }
        }
        System.out.println("=== ⏰ SCHEDULER END ===\n");
    } 
}