package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Enquiry;

import com.visitor.Visitor_Backend.service.EnquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(
	    origins = {
	        "http://localhost:5173",
	        "https://visitor-dun.vercel.app"
	    },
	    allowCredentials = "true"
	)   // Allows your React frontend to connect
public class EnquiryController {

    @Autowired
    private EnquiryService enquiryService;

    @PostMapping("/send")
    public ResponseEntity<?> receiveEnquiry(@RequestBody Enquiry enquiry) {
        try {
            // Validate basic fields
            if (enquiry.getEmail() == null || enquiry.getMessage() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and Message are required"));
            }

            // Pass to service for processing
            enquiryService.processEnquiry(enquiry);

            return ResponseEntity.ok(Map.of("message", "Your message has been sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Something went wrong: " + e.getMessage()));
        }
    }
}