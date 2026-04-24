package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "http://localhost:5173")
public class AppointmentController {

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // --- HELPER: NOTIFICATION LOGIC ---
    // Centralized method to push updates to the Admin WebSocket
    private void sendAdminNotification(String message, Appointment app) {
        try {
            Map<String, String> notification = new HashMap<>();
            notification.put("visitorName", app.getVisitorName());
            notification.put("purpose", app.getPurpose());
            notification.put("dateTime", app.getDate() + " at " + app.getTimeIn());
            notification.put("message", message);
            messagingTemplate.convertAndSend("/topic/admin-notifications", notification);
        } catch (Exception e) {
            System.err.println("WebSocket Notification failed: " + e.getMessage());
        }
    }

    // --- SLOT MANAGEMENT ---

    @PostMapping("/slots")
    public Appointment createSlot(@RequestBody Appointment slot) {
        try {
            // Parse date for Day info
            LocalDate localDate = LocalDate.parse(slot.getDate());
            slot.setDay(localDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            
            // Handle Time formatting
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            LocalTime openTime = LocalTime.parse(slot.getTimeIn().toUpperCase(), tf);
            slot.setTimeOut(openTime.plusMinutes(15).format(tf));

            // --- NEW: INTERVIEW NOTIFICATION LOGIC ---
            if (slot.getPurpose() != null && slot.getPurpose().startsWith("INTERVIEW_REQUEST:")) {
                slot.setAvailable(false); // Interviews are not available for others to book
                slot.setStatus("Pending");
                
                Appointment savedInterview = repository.save(slot);
                
                // Push real-time notification to Admin's Bell Icon
                sendAdminNotification("🚨 NEW INTERVIEW: " + savedInterview.getVisitorName() + " for " + savedInterview.getPurpose().split(":")[1], savedInterview);
                
                return savedInterview;
            }

            // Standard logic for Admin creating empty slots
            slot.setAvailable(true);
            return repository.save(slot);
        } catch (Exception e) {
            return null;
        }
    }
    
    @GetMapping("/available")
    public List<Appointment> getAvailable() {
        List<Appointment> allAvailable = repository.findByAvailableTrue();
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Ensure pattern matches "01:00 PM" exactly
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

        return allAvailable.stream().filter(slot -> {
            try {
                LocalDate slotDate = LocalDate.parse(slot.getDate());
                // .trim() and .toUpperCase() to prevent parsing errors
                String timeStr = slot.getTimeIn().trim().toUpperCase();
                LocalTime slotTime = LocalTime.parse(timeStr, tf);
                
                if (slotDate.isAfter(today)) return true;
                if (slotDate.isEqual(today)) return slotTime.isAfter(now);
                
                return false;
            } catch (Exception e) {
                // If a slot has bad data, skip it instead of breaking the whole list
                System.err.println("Error parsing slot: " + slot.getTimeIn());
                return false;
            }
        }).toList();
    }
    

    @GetMapping("/booked")
    public List<Appointment> getBookedAppointments() {
        return repository.findAll().stream().filter(slot -> !slot.isAvailable()).toList();
    }
    
    @GetMapping("/all")
    public List<Appointment> getAllAppointments() {
        return repository.findAll();
    }

    // --- BOOKING LOGIC ---

    @PutMapping("/book/{id}")
    public Appointment book(@PathVariable String id, @RequestBody Appointment data) {
        return repository.findById(id).map(slot -> {
            List<Appointment> previousVisits = repository.findByEmail(data.getEmail());
            slot.setVisitHistory(previousVisits.isEmpty() ? "1st Visit" : (previousVisits.size() + 1) + "th visit");
            
            // Map data
            slot.setVisitorName(data.getVisitorName());
            slot.setPhone(data.getPhone());
            slot.setEmail(data.getEmail());
            slot.setAddress(data.getAddress());
            slot.setPurpose(data.getPurpose());
            slot.setWhomToMeet(data.getWhomToMeet());
            slot.setVisitorPhoto(data.getVisitorPhoto());
            slot.setAvailable(false);
            slot.setStatus("Pending");

            Appointment saved = repository.save(slot);
         // --- ENHANCED NOTIFICATION LOGIC ---
            String msg = "New Request from " + saved.getVisitorName();
            if (saved.getPurpose() != null && saved.getPurpose().toUpperCase().contains("INTERVIEW")) {
                msg = "🚨 INTERVIEW SCHEDULED: " + saved.getVisitorName();
            }
            
            sendAdminNotification(msg, saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Slot not found"));
    }

    // --- CORRECTED: RESCHEDULE LOGIC ---
    @PutMapping("/reschedule/{oldId}")
    public ResponseEntity<Appointment> reschedule(@PathVariable String oldId, @RequestBody Map<String, Object> payload) {
        return repository.findById(oldId).map(oldSlot -> {
            String newSlotId = (String) payload.get("newSlotId");

            // CASE A: Update details only (Same Slot)
            if (newSlotId == null || newSlotId.equals(oldId)) {
                updateSlotFields(oldSlot, payload);
                Appointment saved = repository.save(oldSlot);
                sendAdminNotification("Details Updated by " + saved.getVisitorName(), saved);
                return ResponseEntity.ok(saved);
            }

            // CASE B: Moving to a NEW Slot
            return repository.findById(newSlotId).map(newSlot -> {
                updateSlotFields(newSlot, payload);
                newSlot.setStatus("Pending");
                newSlot.setAvailable(false);
                repository.save(newSlot);

                // Notify Admin about the Reschedule
                sendAdminNotification("Meeting Rescheduled by " + newSlot.getVisitorName(), newSlot);

                // Clear and free up the old slot
                clearSlotFields(oldSlot);
                repository.save(oldSlot);

                return ResponseEntity.ok(newSlot);
            }).orElse(ResponseEntity.status(404).build());
        }).orElse(ResponseEntity.status(404).build());
    }
    
    

 // --- USER SIDE: CANCEL (FREES UP THE SLOT) ---
 // --- USER SIDE: CANCEL (FREES UP THE SLOT) ---
    @PutMapping("/cancel/{id}") 
    public ResponseEntity<?> deleteAppointment(@PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            // 1. Capture info BEFORE clearing fields
            String visitorName = appointment.getVisitorName();
            String purpose = appointment.getPurpose();
            String date = appointment.getDate();
            String time = appointment.getTimeIn();

            // 2. Clear fields so the slot becomes 'available' again
            clearSlotFields(appointment); 
            
            // 3. Save the reset slot to MongoDB
            Appointment savedSlot = repository.save(appointment); 
            
            // 4. Send Custom Notification
            if (visitorName != null) {
                // REMOVE date and time from the dark text (customMessage)
                String customMessage = "Meeting is cancelled by " + visitorName + " for " + purpose;
                
                // Build the notification map
                Map<String, String> notification = new HashMap<>();
                notification.put("visitorName", visitorName);
                notification.put("message", customMessage); // Dark text
                
                // ADD purpose, date, and time to the light text fields
                notification.put("purpose", purpose); 
                notification.put("dateTime", date + " at " + time); // Light text (metadata)
                
                messagingTemplate.convertAndSend("/topic/admin-notifications", notification);
            }
            
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- 2. ADMIN SIDE: DELETE (REMOVES THE SLOT) ---
    // This stays as @DeleteMapping for your "Manage Slots" page
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            repository.deleteById(id); // Actually removes from MongoDB
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
    
    

    // --- UTILITIES & STATS ---

    private void updateSlotFields(Appointment slot, Map<String, Object> payload) {
        slot.setVisitorName((String) payload.get("visitorName"));
        slot.setPhone((String) payload.get("phone"));
        slot.setEmail((String) payload.get("email"));
        slot.setAddress((String) payload.get("address"));
        slot.setPurpose((String) payload.get("purpose"));
        slot.setWhomToMeet((String) payload.get("whomToMeet"));
        slot.setVisitorPhoto((String) payload.get("visitorPhoto"));
    }

    private void clearSlotFields(Appointment slot) {
        slot.setVisitorName(null);
        slot.setPhone(null);
        slot.setEmail(null);
        slot.setAddress(null);
        slot.setPurpose(null);
        slot.setWhomToMeet(null);
        slot.setVisitorPhoto(null);
        slot.setAvailable(true);
        slot.setStatus(null);
        slot.setVisitHistory(null);
    }

    @GetMapping("/my-meetings")
    public ResponseEntity<List<Appointment>> getMyMeetings(@RequestParam String email) {
        return ResponseEntity.ok(repository.findByEmail(email));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<Appointment> updateStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        return repository.findById(id).map(appointment -> {
            String newStatus = payload.get("status");
            appointment.setStatus(newStatus);
            Appointment saved = repository.save(appointment);

            // --- CUSTOM USER NOTIFICATION LOGIC (EXACTLY AS PROVIDED) ---
            String userMessage = "";
            if ("Approved".equalsIgnoreCase(newStatus)) {
                userMessage = "Meeting Approved! Please arrive at " + saved.getTimeIn() + 
                              " on " + saved.getDate() + ". Kindly reach 5 mins before time.";
            } else if ("Declined".equalsIgnoreCase(newStatus)) {
                userMessage = "Sorry " + saved.getVisitorName() + 
                              ", your meeting for " + saved.getDate() + " is cancelled. " + 
                              "Please select another time slot to meet.";
            }
         // ADDED: Visited notification
            else if ("Visited".equalsIgnoreCase(newStatus)) {
                userMessage = "Thank you for visiting us, " + saved.getVisitorName() + 
                              "! It was a pleasure meeting you. Have a great day ahead!";
            }
            else if ("Not Visited".equalsIgnoreCase(newStatus)) {
                userMessage = "You missed your appointment scheduled for " + saved.getTimeIn() + 
                              ". If you still need to meet, please reschedule a new slot.";
            }

            if (!userMessage.isEmpty()) {
                Map<String, String> userNotif = new HashMap<>();
                userNotif.put("message", userMessage);
                userNotif.put("status", newStatus);
                userNotif.put("type", "STATUS_UPDATE");
                
                // FORCE lowercase and remove spaces to match the Frontend exactly
                String destination = "/topic/user-" + saved.getEmail().toLowerCase().trim();
                System.out.println("DEBUG: Pushing notification to: " + destination);
                
                messagingTemplate.convertAndSend(destination, userNotif);
            }
         // This ensures the bell icon in the Admin Header updates too
            sendAdminNotification("Status updated to " + newStatus + " for " + saved.getVisitorName(), saved);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@RequestParam String email) {
        try {
            List<Appointment> userMeetings = repository.findByEmail(email);
            Map<String, Object> stats = new HashMap<>();
            
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

            // 1. Scheduled Count: Must be APPROVED and in the FUTURE
            long upcomingCount = userMeetings.stream()
                    .filter(m -> "Approved".equalsIgnoreCase(m.getStatus()))
                    .filter(m -> {
                        try {
                            LocalDate mDate = LocalDate.parse(m.getDate());
                            LocalTime mTime = LocalTime.parse(m.getTimeIn().toUpperCase(), tf);
                            if (mDate.isAfter(today)) return true;
                            if (mDate.isEqual(today)) return mTime.isAfter(now);
                            return false;
                        } catch (Exception e) { return false; }
                    }).count();

            // 2. Past Count: Strictly Visited or Completed only
            long pastCount = userMeetings.stream()
                    .filter(m -> "Visited".equalsIgnoreCase(m.getStatus()) || 
                                 "Completed".equalsIgnoreCase(m.getStatus()))
                    .count();

            // 3. Next Meeting: The very next upcoming Approved meeting
            Appointment nextApp = userMeetings.stream()
                    .filter(m -> "Approved".equalsIgnoreCase(m.getStatus()))
                    .filter(m -> {
                        try {
                            LocalDate mDate = LocalDate.parse(m.getDate());
                            LocalTime mTime = LocalTime.parse(m.getTimeIn().toUpperCase(), tf);
                            if (mDate.isAfter(today)) return true;
                            if (mDate.isEqual(today)) return mTime.isAfter(now);
                            return false;
                        } catch (Exception e) { return false; }
                    })
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .findFirst()
                    .orElse(null);

            stats.put("scheduledCount", upcomingCount);
            stats.put("pastCount", pastCount);
            stats.put("nextAppointment", nextApp);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}