package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.Appointment;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVisitorStatusEmail(Appointment appointment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(appointment.getEmail());
            String status = appointment.getStatus();
            helper.setSubject("Appointment Update: " + status);

            // 1. DYNAMIC COLOR & TEXT LOGIC
            String statusColor = "#64748b"; // Default Gray
            String statusMessage = "";
            String actionHtml = ""; // To show/hide buttons

            if (status.equalsIgnoreCase("Approved")) {
                statusColor = "#10b981"; // Green
               
                statusMessage = "Kindly reach 5 mins before time.";
                                
                
                // Show the Confirm/Reschedule/Cancel buttons
                actionHtml = generateActionButtons(appointment.getId());
            }
            else if (status.equalsIgnoreCase("Declined")) {
                statusColor = "#ef4444"; // Red
                statusMessage = "Unfortunately, your request could not be accommodated at this time.";
               
            } 
            else if (status.equalsIgnoreCase("Visited")) {
                statusColor = "#3b82f6"; // Blue
                statusMessage = "Thank you for visiting! We hope you had a productive meeting.";
                actionHtml = ""; // No actions needed after visit
            } 
            else if (status.equalsIgnoreCase("Not Visited")) {
                statusColor = "#f59e0b"; // Orange
                statusMessage = "It looks like you missed your appointment.";
                actionHtml = "<a href='http://localhost:5173/my-meetings?reschedule=" + appointment.getId() + "' style='color: #3b82f6; font-weight: bold;'>REBOOK APPOINTMENT</a>";
            }

            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
            
            // 2. DYNAMIC EMAIL BODY
            String emailBody = 
                "<div style='font-family: Arial, sans-serif; color: #334155; line-height: 1.6; max-width: 600px; margin: 0; padding: 20px;'>" +
                // --- LOGO HEADER ---
                "    <div style='margin-bottom: 30px; border-bottom: 1px solid #e2e8f0; padding-bottom: 20px;'>" +
                "       <table border='0' cellpadding='0' cellspacing='0'>" +
                "           <tr>" +
                "               <td><img src='cid:logo' style='width: 40px; height: 40px;' alt='M'/></td>" +
                "               <td style='font-size: 28px; font-weight: 900; color: #0f172a;'>eetIn</td>" +
                "           </tr>" +
                "       </table>" +
                "    </div>" +

                "    <h2 style='color: #0f172a; font-size: 20px;'>Hello " + appointment.getVisitorName() + ",</h2>" +
                "    <p>Your appointment status has been updated to: " +
                "       <strong style='color: " + statusColor + ";'>" + status.toUpperCase() + "</strong>" +
                "    </p>" +

				//--- DETAILS LIST (Card Styling) ---
				"    <div style='margin: 30px 0; padding: 20px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px;'>" +
				"        <p style='margin: 5px 0; color: #64748b;'><strong>Host:</strong> <span style='color: #0f172a;'>" + appointment.getWhomToMeet() + "</span></p>" +
				"        <p style='margin: 5px 0; color: #64748b;'><strong>Date:</strong> <span style='color: #0f172a;'>" + appointment.getDate() + "</span></p>" +
				"        <p style='margin: 5px 0; color: #64748b;'><strong>Time:</strong> <span style='color: #0f172a;'>" + appointment.getTimeIn() + " - " + appointment.getTimeOut() + "</span></p>" +
				"        <p style='margin: 5px 0; color: #64748b;'><strong>Purpose:</strong> <span style='color: #0f172a;'>" + appointment.getPurpose() + "</span></p>" +
				"    </div>" +

//--- DYNAMIC ACTION SECTION ---
"    <div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #e2e8f0;'>" +
 // 1. Bold Status Message (Dark)
"        <p style='color: #0f172a; font-weight: bold; font-size: 16px; margin: 0 0 4px 0;'>" + 
          statusMessage + 
"        </p>" +
 // 2. Secondary Instruction (Light Slate Gray)
"        <p style='color: #64748b; font-size: 14px; margin: 0 0 20px 0;'>" + 
"            If you need to change these details, please contact the office." + 
"        </p>" +
 // 3. Buttons (Confirm / Reschedule / Cancel)
"        <div style='margin-top: 15px;'>" + actionHtml + "</div>" +
"    </div>" +

"    <div style='margin-top: 50px; font-size: 11px; color: #94a3b8;'>" +
"      Sent via MeetIn Management System • " + currentTime + "" +
"    </div>" +
"</div>";

            helper.setText(emailBody, true);
            
            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            if (logo.exists()) { helper.addInline("logo", logo); }

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email failed: " + e.getMessage());
        }
    }

    // Helper method to keep code clean
    private String generateActionButtons(String appointmentId) {
        String backendBaseUrl = "http://localhost:8080/api/appointments/action";
        String frontendUrl = "http://localhost:5173/my-meetings";
        
        return "<a href='" + backendBaseUrl + "/confirm/" + appointmentId + "' style='text-decoration: none; color: #10b981; font-weight: bold; margin-right: 15px;'>CONFIRM</a>" +
               "<a href='" + frontendUrl + "?reschedule=" + appointmentId + "' style='text-decoration: none; color: #3b82f6; font-weight: bold; margin-right: 15px;'>RESCHEDULE</a>" +
               "<a href='" + backendBaseUrl + "/cancel/" + appointmentId + "' style='text-decoration: none; color: #ef4444; font-weight: bold;'>CANCEL</a>";
    }
}