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

    @Value("${brevo.api.key}")
    private String BREVO_API_KEY;

    @Value("${brevo.sender.email}")
    private String SENDER_EMAIL;

    // ✅ Create / Schedule Meeting
    @PostMapping("/schedule")
    public ResponseEntity<String> scheduleMeeting(@RequestBody InternalMeeting meeting) {
        internalMeetingRepository.save(meeting);
        
        for (TeamMember member : meeting.getSelectedEmployees()) {
            Map<String, String> notif = new HashMap<>();
            notif.put("message", "New Team Meeting: " + meeting.getProjectName());
            notif.put("status", "Approved");
            
            String topic = "/topic/user-" + member.getEmail().toLowerCase().trim();
            messagingTemplate.convertAndSend(topic, notif);

            sendEmailViaBrevo(member.getEmail(), meeting);
        }
        
        return ResponseEntity.ok("Meeting Scheduled Successfully!");
    }
    
    // ✅ Read All Meetings
    @GetMapping("/all")
    public List<InternalMeeting> getAllInternalMeetings() {
        return internalMeetingRepository.findAll(); 
    }

    // ✅ Delete Single Meeting Log
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMeeting(@PathVariable("id") String id) {
        try {
            if (internalMeetingRepository.existsById(id)) {
                internalMeetingRepository.deleteById(id);
                return ResponseEntity.ok().body(Map.of("message", "Meeting log deleted successfully."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Meeting record not found in database."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete meeting: " + e.getMessage()));
        }
    }

    // ✅ Clear All Meeting Logs
    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllMeetings() {
        try {
            internalMeetingRepository.deleteAll();
            return ResponseEntity.ok().body(Map.of("message", "All meeting logs cleared successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear records: " + e.getMessage()));
        }
    }

    // ✅ Private Brevo Transactional Email Method
    private void sendEmailViaBrevo(String toEmail, InternalMeeting meeting) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", BREVO_API_KEY);
            headers.set("accept", "application/json");

            String formattedTime = meeting.getStartTime(); 
            try {
                if (meeting.getStartTime() != null && !meeting.getStartTime().isEmpty()) {
                    java.time.LocalTime time = java.time.LocalTime.parse(meeting.getStartTime());
                    java.time.format.DateTimeFormatter amPmFormatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
                    formattedTime = time.format(amPmFormatter);
                }
            } catch (Exception parseEx) {
                System.err.println("Time parsing failed, using raw value: " + parseEx.getMessage());
            }

            String htmlContent = "<div style='font-family: Arial, sans-serif; border: 1px solid #ddd; padding: 20px; border-radius: 10px;'>" +
                "<h2 style='color: #E25A45;'>Internal Team Meeting</h2>" +
                "<p>You have been invited to a meeting for <b>" + meeting.getProjectName() + "</b></p>" +
                "<p><b>Date:</b> " + meeting.getMeetingDate() + "</p>" +
                "<p><b>Time:</b> " + formattedTime + "</p>" +
                "<br><a href='https://visitor-dun.vercel.app' style='background: #0F172A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>View Dashboard</a>" +
                "</div>";

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "Visitor Management", "email", SENDER_EMAIL));
            body.put("to", new Object[]{Map.of("email", toEmail)});
            body.put("subject", "New Meeting Invitation: " + meeting.getProjectName());
            body.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);

        } catch (Exception e) {
            System.err.println("Brevo Meeting Email failed to " + toEmail + ": " + e.getMessage());
        }
    }
}