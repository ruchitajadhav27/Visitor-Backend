package com.visitor.Visitor_Backend.service;

import java.util.List;
import java.util.ArrayList;
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

    @Scheduled(cron = "0 * * * * ?")
    public void checkTimeOutMeetings() {
        
        LocalTime now = LocalTime.now();
        // Dono format nikalenge: "12:30 PM" aur "12:30 pm"
        String timeUpper = now.format(DateTimeFormatter.ofPattern("hh:mm a"));
        String timeLower = timeUpper.toLowerCase();
        
        System.out.println("⏰ Background Scheduler running. Checking for: [" + timeUpper + "] or [" + timeLower + "]");
        
        // Dono formats ke appointments dhoondhenge taaki database entry mismatch na ho
        List<Appointment> expiringMeetings = new ArrayList<>();
        expiringMeetings.addAll(appointmentRepository.findByStatusAndTimeOut("Approved", timeUpper));
        expiringMeetings.addAll(appointmentRepository.findByStatusAndTimeOut("Approved", timeLower));

        if (expiringMeetings.isEmpty()) {
            System.out.println("ℹ️ No approved meetings expiring at this exact minute.");
            return;
        }

        System.out.println("🎯 Found " + expiringMeetings.size() + " expiring meeting(s)!");

        for (Appointment app : expiringMeetings) {
            String userEmail = app.getEmail(); 

            if (userEmail == null || userEmail.isEmpty()) {
                System.out.println("⚠️ Warning: No email found for visitor: " + app.getVisitorName());
                continue;
            }

            String cleanEmail = userEmail.toLowerCase().trim();
            System.out.println("🔍 Scheduler fetching tokens for: " + cleanEmail);
            
            List<SubscriptionToken> tokens = tokenRepository.findByEmail(cleanEmail);
            
            if (tokens == null || tokens.isEmpty()) {
                System.out.println("⚠️ No active subscription web tokens found in DB for " + cleanEmail);
                continue;
            }

            // Standard stringified JSON payload structure compatible with sw.js
            String jsonPayload = "{\"title\":\"⏰ TIME IS UP!\",\"body\":\"Your meeting time has ended, " + app.getVisitorName() + ".\"}";

            for (SubscriptionToken token : tokens) {
                try {
                    System.out.println("📤 Triggering web push to endpoint: " + token.getEndpoint().substring(0, 40) + "...");
                    webPushService.sendPushNotification(
                        token.getEndpoint(), 
                        token.getP256dh(), 
                        token.getAuth(), 
                        jsonPayload
                    );
                    System.out.println("🚀 Desktop Push Delivered successfully to endpoint server!");
                } catch (Exception e) {
                    System.err.println("❌ Critical delivery failure for token: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    } 
}