package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.Appointment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String BREVO_API_KEY;

    @Value("${brevo.sender.email}")
    private String SENDER_EMAIL;

    public void sendVisitorStatusEmail(Appointment appointment) {

        try {
            String status = appointment.getStatus();
            String statusColor = "#64748b"; 
            String statusMessage = "";
            String actionHtml = "";

            if (status.equalsIgnoreCase("Approved")) {
                statusColor = "#10b981";
                statusMessage = "Kindly reach 5 mins before time.";
                actionHtml = generateActionButtons(appointment.getId());
            }
            else if (status.equalsIgnoreCase("Declined")) {
                statusColor = "#ef4444";
                statusMessage = "Unfortunately, your request could not be accommodated at this time.";
            }
            else if (status.equalsIgnoreCase("Visited")) {
                statusColor = "#3b82f6";
                statusMessage = "Thank you for visiting! We hope you had a productive meeting.";
                actionHtml = "";
            }
            else if (status.equalsIgnoreCase("Not Visited")) {
                statusColor = "#f59e0b";
                statusMessage = "It looks like you missed your appointment.";
                // ✅ FIXED: Changed from localhost:5173 to your live production Vercel dashboard
                actionHtml = "<a href='https://visitor-dun.vercel.app/my-meetings?reschedule="
                                + appointment.getId()
                                + "' style='color: #3b82f6; font-weight: bold;'>REBOOK APPOINTMENT</a>";
            }

            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

            String emailBody =
                    "<div style='font-family: Arial, sans-serif; color: #334155; line-height: 1.6; max-width: 600px; margin: 0; padding: 20px;'>" +
                            "<div style='margin-bottom: 30px; border-bottom: 1px solid #e2e8f0; padding-bottom: 20px;'>" +
                            "       <table border='0' cellpadding='0' cellspacing='0'>" +
                            "           <tr>" +
                            "<td><img src='https://visitor-backend-3-fbww.onrender.com/images/logo.png' style='width: 40px; height: 40px;' alt='MeetIn Logo'/></td>" +
                            "               <td style='font-size: 28px; font-weight: 900; color: #0f172a;'>eetIn</td>" +
                            "           </tr>" +
                            "       </table>" +
                            "    </div>" +
                            "<h2 style='color: #0f172a; font-size: 20px;'>Hello " + appointment.getVisitorName() + ",</h2>" +
                            "<p>Your appointment status has been updated to: <strong style='color: " + statusColor + ";'>" + status.toUpperCase() + "</strong></p>" +
                            "<div style='margin: 30px 0; padding: 20px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px;'>" +
                            "<p style='margin: 5px 0; color: #64748b;'><strong>Host:</strong> <span style='color: #0f172a;'>" + appointment.getWhomToMeet() + "</span></p>" +
                            "<p style='margin: 5px 0; color: #64748b;'><strong>Date:</strong> <span style='color: #0f172a;'>" + appointment.getDate() + "</span></p>" +
                            "<p style='margin: 5px 0; color: #64748b;'><strong>Time:</strong> <span style='color: #0f172a;'>" + appointment.getTimeIn() + " - " + appointment.getTimeOut() + "</span></p>" +
                            "<p style='margin: 5px 0; color: #64748b;'><strong>Purpose:</strong> <span style='color: #0f172a;'>" + appointment.getPurpose() + "</span></p>" +
                            "</div>" +
                            "<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #e2e8f0;'>" +
                            "<p style='color: #0f172a; font-weight: bold; font-size: 16px; margin: 0 0 4px 0;'>" + statusMessage + "</p>" +
                            "<p style='color: #64748b; font-size: 14px; margin: 0 0 20px 0;'>If you need to change these details, please contact the office.</p>" +
                            "<div style='margin-top: 15px;'>" + actionHtml + "</div>" +
                            "</div>" +
                            "<div style='margin-top: 50px; font-size: 11px; color: #94a3b8;'>Sent via MeetIn Management System • " + currentTime + "</div>" +
                            "</div>";

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", BREVO_API_KEY);
            headers.set("accept", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "MeetIn", "email", SENDER_EMAIL));
            body.put("to", new Object[]{Map.of("email", appointment.getEmail())});
            body.put("subject", "Appointment Update: " + status);
            body.put("htmlContent", emailBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("BREVO SUCCESS RESPONSE: " + response.getBody());

        } catch (Exception e) {
            System.out.println("EMAIL FAILED: " + e.getMessage());
        }
    }

    private String generateActionButtons(String appointmentId) {
        // ✅ Direct Render Base Route Definition
        String backendBaseUrl = "https://visitor-backend-3-fbww.onrender.com/api/appointments/action";
        String frontendUrl = "https://visitor-dun.vercel.app/my-meetings";

        return "<a href='" + backendBaseUrl + "/confirm/" + appointmentId + "' style='text-decoration: none; color: #10b981; font-weight: bold; margin-right: 15px;'>CONFIRM</a>"
                + "<a href='" + frontendUrl + "?reschedule=" + appointmentId + "' style='text-decoration: none; color: #3b82f6; font-weight: bold; margin-right: 15px;'>RESCHEDULE</a>"
                + "<a href='" + backendBaseUrl + "/cancel/" + appointmentId + "' style='text-decoration: none; color: #ef4444; font-weight: bold;'>CANCEL</a>";
    }
    
    
    public void sendFollowUpReminderEmail(Appointment appointment) {
        try {
            String emailBody =
                "<div style='font-family: Arial, sans-serif; color: #334155; line-height: 1.6; max-width: 600px; margin: 0; padding: 20px;'>" +
                    "<div style='margin-bottom: 30px; border-bottom: 1px solid #e2e8f0; padding-bottom: 20px;'>" +
                        "<table border='0' cellpadding='0' cellspacing='0'>" +
                            "<tr>" +
                                "<td><img src='https://visitor-backend-3-fbww.onrender.com/images/logo.png' style='width: 40px; height: 40px;' alt='MeetIn Logo'/></td>" +
                                "<td style='font-size: 28px; font-weight: 900; color: #0f172a;'>eetIn</td>" +
                            "</tr>" +
                        "</table>" +
                    "</div>" +
                    "<h2 style='color: #e25a45; font-size: 22px; margin-bottom: 4px;'>⏰ Meeting Reminder (15 Mins Left!)</h2>" +
                    "<p>Hello <strong>" + appointment.getVisitorName() + "</strong>,</p>" +
                    "<p>This is a follow-up reminder that your meeting is starting in exactly 15 minutes. Please be ready.</p>" +
                    
                    // Live tracking components variables
                    "<div style='margin: 25px 0; padding: 20px; background-color: #fef2f2; border: 1px solid #fca5a5; border-radius: 16px;'>" +
                        "<h3 style='margin: 0 0 15px 0; color: #991b1b; font-size: 16px;'>📋 LIVE QUEUE PASS INFO</h3>" +
                        "<p style='margin: 6px 0;'><strong>🎫 Token Number:</strong> <span style='color: #0f172a; font-family: monospace; font-size: 16px; font-weight: bold;'>" + appointment.getTokenNumber() + "</span></p>" +
                        "<p style='margin: 6px 0;'><strong>🚶 Position in Queue:</strong> <span style='color: #e25a45; font-size: 16px; font-weight: bold;'>#" + appointment.getQueuePosition() + "</span></p>" +
                        "<p style='margin: 6px 0;'><strong>🎯 Purpose:</strong> <span style='color: #0f172a;'>" + appointment.getPurpose() + "</span></p>" +
                        "<p style='margin: 6px 0;'><strong>👤 Meeting With:</strong> <span style='color: #0f172a;'>" + appointment.getWhomToMeet() + "</span></p>" +
                        "<p style='margin: 6px 0;'><strong>⏱️ Scheduled Time:</strong> <span style='color: #0f172a;'>" + appointment.getTimeIn() + "</span></p>" +
                    "</div>" +
                    
                    "<p style='color: #64748b; font-size: 13px;'>If you're already at the venue, please wait at the lounge area. Our system will notify you once your host is available.</p>" +
                    "<div style='margin-top: 40px; font-size: 11px; color: #94a3b8;'>Sent automatically via MeetIn Engine Scheduler</div>" +
                "</div>";

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", BREVO_API_KEY);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "MeetIn Reminders", "email", SENDER_EMAIL));
            body.put("to", new Object[]{Map.of("email", appointment.getEmail())});
            body.put("subject", "⏰ Live Queue Update: Token " + appointment.getTokenNumber() + " (15 Mins Left!)");
            body.put("htmlContent", emailBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("⏰ REMINDER EMAIL DESPATCHED SUCCESSFULLY TO: " + appointment.getEmail());

        } catch (Exception e) {
            System.err.println("REMINDER SCHEDULER SYSTEM MAIL FAILURE: " + e.getMessage());
        }
    }
    
    
}