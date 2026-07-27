package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments/action")
@CrossOrigin(
    originPatterns = {
        "http://localhost:5173",
        "https://visitor-dun.vercel.app",
        "https://*.vercel.app"
    },
    allowCredentials = "true"
)
public class AppointmentActionController {

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private AppointmentController mainController;

    @GetMapping("/{action}/{id}")
    public String handleEmailAction(@PathVariable String action, @PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            String title;
            String message;
            String color;

            if ("confirm".equalsIgnoreCase(action)) {
                // 1. Logic Execute kiya
                appointment.setStatus("Approved");
                appointment.setVisitHistory("Confirmed via Email");
                repository.save(appointment);
                
                // 2. Specific Admin Notification
                mainController.sendConfirmationNotification(appointment);
                
                // 3. UI Content
                title = "Appointment Confirmed!";
                message = "Thank you for confirming your visit, " + appointment.getVisitorName() + ". We look forward to seeing you.";
                color = "#10b981"; // Success Green
            
            } else if ("cancel".equalsIgnoreCase(action)) {
                // 1. Logic Execute kiya
                mainController.sendCancellationNotification(appointment);
                mainController.deleteAppointment(id);
                
                // 2. UI Content
                title = "Meeting Cancelled";
                message = "The appointment slot has been released. Thank you for letting us know.";
                color = "#ef4444"; // Error Red
            
            } else {
                title = "Invalid Request";
                message = "The link you followed is invalid or has expired.";
                color = "#6b7280"; // Gray
            }

            // ✅ PURE HTML RESPONSE: Changed button link to point directly to main production landing page
            return "<html>" +
                   "<body style='font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f3f4f6;'>" +
                   "  <div style='background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; max-width: 400px;'>" +
                   "    <div style='font-size: 50px; margin-bottom: 20px;'> " + ("confirm".equals(action) ? "✅" : "❌") + " </div>" +
                   "    <h1 style='color: " + color + "; margin-bottom: 10px;'>" + title + "</h1>" +
                   "    <p style='color: #4b5563; line-height: 1.6;'>" + message + "</p>" +
                   "    <hr style='border: 0; border-top: 1px solid #e5e7eb; margin: 20px 0;'>" +
                   "    <a href='https://visitor-dun.vercel.app' style='display: inline-block; background-color: #0f172a; color: white; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-size: 14px; margin-bottom: 12px;'>Go to Dashboard</a>" +
                   "    <p style='font-size: 12px; color: #9ca3af;'>You can close this window now.</p>" +
                   "  </div>" +
                   "</body></html>";

        }).orElse("<html><body style='text-align:center; font-family: Arial, sans-serif; padding-top:50px; background-color: #f3f4f6;'>" +
                 "  <div style='display: inline-block; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);'>" +
                 "    <h1 style='color: #ef4444;'>Appointment Not Found</h1>" +
                 "    <p style='color: #4b5563;'>This appointment link is invalid or has already been cleared.</p>" +
                 "    <a href='https://visitor-dun.vercel.app' style='color: #0f172a; font-weight: bold; text-decoration: none;'>Go to Visitor System</a>" +
                 "  </div>" +
                 "</body></html>");
    }
}