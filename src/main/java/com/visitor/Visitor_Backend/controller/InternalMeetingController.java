package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.InternalMeeting;
import com.visitor.Visitor_Backend.model.TeamMember;
import com.visitor.Visitor_Backend.repository.InternalMeetingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.mail.internet.MimeMessage;
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
    private JavaMailSender mailSender;
    
    @Autowired
    private InternalMeetingRepository internalMeetingRepository;

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

            sendEmail(member.getEmail(), meeting);
        } // <--- Added this missing closing brace
        
        return "Meeting Scheduled Successfully!";
    }
    
    @GetMapping("/all")
    public List<InternalMeeting> getAllInternalMeetings() {
        return internalMeetingRepository.findAll(); 
        // This assumes you have an InternalMeetingRepository
    }

    private void sendEmail(String toEmail, InternalMeeting meeting) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(toEmail);
            helper.setSubject("New Meeting Invitation: " + meeting.getProjectName());
            
            // Attractive HTML Content
            String htmlContent = "<div style='font-family: Arial; border: 1px solid #ddd; padding: 20px; border-radius: 10px;'>" +
                "<h2 style='color: #E25A45;'>Internal Team Meeting</h2>" +
                "<p>You have been invited to a meeting for <b>" + meeting.getProjectName() + "</b></p>" +
                "<p><b>Date:</b> " + meeting.getMeetingDate() + "</p>" +
                "<p><b>Time:</b> " + meeting.getStartTime() + "</p>" +
                "<br><a href='http://localhost:5173' style='background: #0F172A; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>View Dashboard</a>" +
                "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email failed to: " + toEmail);
        }
    }
}