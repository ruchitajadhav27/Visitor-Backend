package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.InternalMeeting;
import com.visitor.Visitor_Backend.model.TeamMember;
import com.visitor.Visitor_Backend.repository.InternalMeetingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal-meetings")
@CrossOrigin(origins = {
	    "http://localhost:5173",
	    "https://visitor-dun.vercel.app"
	})
public class InternalMeetingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private InternalMeetingRepository internalMeetingRepository;

    // ✅ Injecting Brevo keys from application.properties
    @Value("${brevo.api.key}")
    private String BREVO_API_KEY;

    @Value("${brevo.sender.email}")
    private String SENDER_EMAIL;

    @PostMapping("/schedule")
    public String scheduleMeeting(@RequestBody InternalMeeting meeting) {
        // 0. Save to DB
        internalMeetingRepository.save(meeting);
        
        // 1. Loop through TeamMember objects
        for (TeamMember member : meeting.getSelectedEmployees()) {
            Map<String, String> notif = new HashMap<>();
            notif.put("message", "New Team Meeting: " + meeting.getProjectName());
            notif.put("status", "Approved");
            
            String topic = "/topic/user-" + member.getEmail().toLowerCase().trim();
            messagingTemplate.convertAndSend(topic, notif);

            // ✅ Call the updated Brevo email method
            sendEmailViaBrevo(member.getEmail(), meeting);
        }
        
        return "Meeting Scheduled Successfully!";
    }
    
    @GetMapping("/all")
    public List<InternalMeeting> getAllInternalMeetings() {
        return internalMeetingRepository.findAll(); 
    }

    private void sendEmailViaBrevo(String toEmail, InternalMeeting meeting) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", BREVO_API_KEY);
            headers.set("accept", "application/json");

            // 🕒 TIME CONVERSION LOGIC (AM/PM ke liye)
            String formattedTime = meeting.getStartTime(); // Fallback agar parse na ho paye
            try {
                if (meeting.getStartTime() != null && !meeting.getStartTime().isEmpty()) {
                    // Agar frontend "14:30" bhej raha hai, toh use LocalTime mein parse karenge
                    java.time.LocalTime time = java.time.LocalTime.parse(meeting.getStartTime());
                    // Ise "02:30 PM" format mein convert karenge
                    java.time.format.DateTimeFormatter amPmFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
                    formattedTime = time.format(amPmFormatter);
                }
            } catch (Exception parseEx) {
                System.err.println("Time parsing failed, using raw value: " + parseEx.getMessage());
            }

            // Attractive HTML Content (Yahan meeting.getStartTime() ki jagah formattedTime use kiya hai)
            String htmlContent = "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; padding: 20px; border-radius: 10px;'>" +
                "<h2 style='color: #E25A45;'>Internal Team Meeting</h2>" +
                "<p>You have been invited to a meeting for <b>" + meeting.getProjectName() + "</b></p>" +
                "<p><b>Date:</b> " + meeting.getMeetingDate() + "</p>" +
                "<p><b>Time:</b> " + formattedTime + "</p>" + // ✅ Displaying formatted AM/PM time
                "<br><a href='https://visitor-dun.vercel.app' style='background: #0F172A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>View Dashboard</a>" +
                "</div>";

            // Preparing Brevo API Request Body
            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "Visitor Management", "email", SENDER_EMAIL));
            body.put("to", new Object[]{Map.of("email", toEmail)});
            body.put("subject", "New Meeting Invitation: " + meeting.getProjectName());
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("BREVO MEETING EMAIL SUCCESS for " + toEmail + ": " + response.getBody());

        } catch (Exception e) {
            System.err.println("Brevo Meeting Email failed to " + toEmail + ": " + e.getMessage());
        }
    }
}