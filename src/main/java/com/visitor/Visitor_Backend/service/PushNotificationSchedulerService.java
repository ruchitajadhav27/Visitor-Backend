package com.visitor.Visitor_Backend.service;

import java.util.List;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
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

        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

        // ✅ Aaj ki date
        String todayStr = LocalDate.now(indiaZone).toString();

        LocalTime now = LocalTime.now(indiaZone);
        String timeUpper = now.format(DateTimeFormatter.ofPattern("hh:mm a"));
        String timeLower = timeUpper.toLowerCase();
        String time24 = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        System.out.println("\n=== ⏰ SCHEDULER START ===");
        System.out.println("📅 Today's Date: " + todayStr);
        System.out.println("   👉 Upper: [" + timeUpper + "]");
        System.out.println("   👉 Lower: [" + timeLower + "]");
        System.out.println("   👉 24-Hr: [" + time24 + "]");

        long totalApproved = appointmentRepository.count();
        System.out.println("📊 Total appointments in collection: " + totalApproved);

        // ✅ Date + Time dono se match karega ab
        List<Appointment> expiringMeetings = appointmentRepository.findByStatusAndDateAndTimeOut("Approved", todayStr, timeUpper);
        if (expiringMeetings.isEmpty()) {
            expiringMeetings = appointmentRepository.findByStatusAndDateAndTimeOut("Approved", todayStr, timeLower);
        }
        if (expiringMeetings.isEmpty()) {
            expiringMeetings = appointmentRepository.findByStatusAndDateAndTimeOut("Approved", todayStr, time24);
        }

        if (expiringMeetings.isEmpty()) {
            System.out.println("ℹ️ No matches found for today's date + time.");
            System.out.println("=== ⏰ SCHEDULER END ===\n");
            return;
        }

        System.out.println("🎯 MATCH FOUND! Expiring meeting(s): " + expiringMeetings.size());

        for (Appointment app : expiringMeetings) {
            String userEmail = app.getEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                System.out.println("⚠️ Skipped: No email for: " + app.getVisitorName());
                continue;
            }

            String cleanEmail = userEmail.toLowerCase().trim();
            List<SubscriptionToken> tokens = tokenRepository.findByEmail(cleanEmail);

            if (tokens == null || tokens.isEmpty()) {
                System.out.println("⚠️ No tokens found for: " + cleanEmail);
                continue;
            }

            String safeVisitorName = app.getVisitorName() != null ? app.getVisitorName().replace("\"", "\\\"") : "Visitor";

            String jsonPayload = String.format(
                "{\"title\":\"⏰ TIME IS UP!\",\"body\":\"Your meeting time has ended, %s.\"}",
                safeVisitorName
            );

            for (SubscriptionToken token : tokens) {
                try {
                    webPushService.sendPushNotification(
                        token.getEndpoint(),
                        token.getP256dh(),
                        token.getAuth(),
                        jsonPayload
                    );
                    System.out.println("🚀 Push delivered to: " + cleanEmail);
                } catch (Exception e) {
                    System.err.println("❌ Push failed: " + e.getMessage());
                }
            }
        }
        System.out.println("=== ⏰ SCHEDULER END ===\n");
    }
}
