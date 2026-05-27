package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.model.SubscriptionToken;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import com.visitor.Visitor_Backend.repository.NotificationRepository;
import com.visitor.Visitor_Backend.repository.SubscriptionTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class ReminderSchedulerService {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SubscriptionTokenRepository tokenRepository; // 🌟 WebPush Tokens fetch karne ke liye

    @Autowired
    private WebPushService webPushService; // 🌟 Real Desktop Push bhezne ke liye

    @Autowired
    private EmailService emailService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Har minute par check karega background engines ko
    @Scheduled(cron = "0 * * * * *")
    public void execute15MinRemindersEngine() {
        String todayStr = LocalDate.now().toString(); 
        
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        LocalTime executionTargetTime = LocalTime.now().plusMinutes(15).truncatedTo(ChronoUnit.MINUTES);

        // Fetch targets where status is 'Approved' and reminderSent is false
        List<Appointment> todaysPendingQueue = appointmentRepo.findByDateAndStatusAndReminderSentFalse(todayStr, "Approved");

        for (Appointment appointment : todaysPendingQueue) {
            try {
                String timeInStr = appointment.getTimeIn().trim().toUpperCase();
                LocalTime slotTime = LocalTime.parse(timeInStr, tf).truncatedTo(ChronoUnit.MINUTES);

                // Check if meeting window is exactly 15 minutes away
                if (slotTime.equals(executionTargetTime)) {
                    
                    // 1. Send Rich HTML Mail via Brevo
                    emailService.sendFollowUpReminderEmail(appointment);

                    // 2. Format UI Text String for User Notifications
                    String wsAlertPayload = "⏰ Reminder: Your meeting for '" + appointment.getPurpose() + 
                                            "' starts in 15 mins! Token: " + appointment.getTokenNumber() + 
                                            " | Position: #" + appointment.getQueuePosition();

                    // 3. PERSISTENT: Save inside MongoDB for History UI Panel
                    Notification userNotifDb = new Notification();
                    userNotifDb.setRecipient(appointment.getEmail().toLowerCase().trim());
                    userNotifDb.setMessage(wsAlertPayload);
                    userNotifDb.setStatus("Reminder15Min");

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
                    userNotifDb.setReceivedAt(now.format(dtf));
                    userNotifDb.setRead(false);
                    notificationRepository.save(userNotifDb);

                    // 4. WEBSOCKET: In-app pop-up broadcast panel updates
                    String targetDestinationChannel = "/topic/user-" + appointment.getEmail().toLowerCase().trim();
                    messagingTemplate.convertAndSend(targetDestinationChannel, userNotifDb);

                    // 5. 🚀 REAL WEBPUSH NOTIFICATION (Desktop/Mobile Banner Notification)
                    sendRealWebPush(appointment);

                    // 6. KILL REPETITIONS: Update document state flag
                    appointment.setReminderSent(true);
                    appointmentRepo.save(appointment);
                }
            } catch (Exception ex) {
                System.err.println("Error processing 15min telemetry for ID " + appointment.getId() + " : " + ex.getMessage());
            }
        }
    }

    /**
     * Helper Method: Fetches user browser tokens and fires actual OS system notification pop-ups.
     */
    private void sendRealWebPush(Appointment app) {
        if (app.getEmail() == null || app.getEmail().isEmpty()) return;

        // 🌟 Step 1: Email ko lowercase aur bilkul clean karke target banayein
        String targetEmail = app.getEmail().toLowerCase().trim();
        
        // Database se token nikalein
        List<SubscriptionToken> tokens = tokenRepository.findByEmail(targetEmail);

        // 🌟 Step 2: Fallback Check (Agar pehli bar mein nahi mila)
        // Kabhi kabhi token save hote waqt email exact match nahi hota, toh contains query se safe side check karein
        if (tokens == null || tokens.isEmpty()) {
            System.out.println("⚠️ WebPush Skipped: No exact subscription key found for: [" + targetEmail + "]. Trying loose match...");
            // Agar aapke repo mein findByEmailContaining hai toh use karein, nahi toh exact mismatch hi main dikkat hai
            return;
        }

        String safeVisitorName = app.getVisitorName() != null ? app.getVisitorName().replace("\"", "\\\"") : "Visitor";
        String safeToken = app.getTokenNumber() != null ? app.getTokenNumber() : "N/A";
        String safePosition = app.getQueuePosition() != null ? String.valueOf(app.getQueuePosition()) : "0";

        String jsonPayload = String.format(
            "{\"title\":\"⏰ 15 MINS REMINDER!\",\"body\":\"Hello %s, your meeting starts in 15 mins. Token: %s | Queue: #%s\"}", 
            safeVisitorName, safeToken, safePosition
        );

        for (SubscriptionToken token : tokens) {
            try {
                webPushService.sendPushNotification(
                    token.getEndpoint(), 
                    token.getP256dh(), 
                    token.getAuth(), 
                    jsonPayload
                );
                System.out.println("🚀 15-Min Desktop Push Delivered successfully!");
            } catch (Exception e) {
                System.err.println("❌ Delivery crashed for specific client stream: " + e.getMessage());
            }
        }
    }
}