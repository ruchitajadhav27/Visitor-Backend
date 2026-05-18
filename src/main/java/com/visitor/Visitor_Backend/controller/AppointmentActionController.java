package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/appointments/action")
@CrossOrigin(
    origins = {
        "http://localhost:5173",
        "https://visitor-dun.vercel.app"
    },
    allowCredentials = "true"
)
public class AppointmentActionController {

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private AppointmentController mainController;

    // ✅ Inject production frontend URL from application.properties
    // If not set, it will fallback to your Vercel production app link
    @Value("${frontend.url:https://visitor-dun.vercel.app}")
    private String frontendUrl;

    @GetMapping("/{action}/{id}")
    public RedirectView handleEmailAction(@PathVariable String action, @PathVariable String id) {
        RedirectView redirectView = new RedirectView();
        
        // Find appointment and execute the logic
        repository.findById(id).ifPresentOrElse(appointment -> {
            
            if ("confirm".equalsIgnoreCase(action)) {
                // 1. Logic
                appointment.setStatus("Approved");
                appointment.setVisitHistory("Confirmed via Email");
                repository.save(appointment);
                
                // 2. Specific Admin Notification
                mainController.sendConfirmationNotification(appointment);
                
                // 3. Point to Frontend Success Status Route
                redirectView.setUrl(frontendUrl + "/appointment-status?status=success&visitor=" + appointment.getVisitorName());
            
            } else if ("cancel".equalsIgnoreCase(action)) {
                // 1. Logic
                mainController.sendCancellationNotification(appointment);
                mainController.deleteAppointment(id);
                
                // 2. Point to Frontend Cancelled Status Route
                redirectView.setUrl(frontendUrl + "/appointment-status?status=cancelled");
            
            } else {
                // Invalid Action
                redirectView.setUrl(frontendUrl + "/appointment-status?status=invalid");
            }
            
        }, () -> {
            // Appointment ID not found in database
            redirectView.setUrl(frontendUrl + "/appointment-status?status=notfound");
        });

        return redirectView;
    }
}