package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Appointment;
import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import com.visitor.Visitor_Backend.repository.NotificationRepository;
import com.visitor.Visitor_Backend.service.EmailService;

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
@CrossOrigin(originPatterns = {
    "http://localhost:5173",
    "https://visitor-dun.vercel.app",
    "https://*.vercel.app"
})
public class AppointmentController {

    @Autowired
    private AppointmentRepository repository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private EmailService emailService;

    // --- HELPER: NOTIFICATION LOGIC ---
    private void sendAdminNotification(String message, Appointment app) {
        try {
            Notification dbNotif = new Notification();
            dbNotif.setRecipient("ADMIN"); 
            dbNotif.setMessage(message);
            dbNotif.setVisitorName(app.getVisitorName());
            dbNotif.setPurpose(app.getPurpose());
            dbNotif.setDateTime(app.getDate() + " at " + app.getTimeIn());
            
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH);
            dbNotif.setReceivedAt(now.format(formatter));
            dbNotif.setRead(false);

            notificationRepository.save(dbNotif);
            messagingTemplate.convertAndSend("/topic/admin-notifications", dbNotif);
            
        } catch (Exception e) {
            System.err.println("Notification failed: " + e.getMessage());
        }
    }

    // --- SLOT MANAGEMENT & CREATION ---

    // Cleaned up unified endpoint to handle both Admin Empty Slots AND direct User Interview requests
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

            // CASE A: If this request is coming from your React InterviewForm
            if (slot.getPurpose() != null && slot.getPurpose().toUpperCase().contains("INTERVIEW")) {
                slot.setAvailable(false); // Interviews should not be public for others to book
                slot.setStatus("Pending");
                
                List<Appointment> previousVisits = repository.findByEmail(slot.getEmail());
                slot.setVisitHistory(previousVisits.isEmpty() ? "1st Visit" : (previousVisits.size() + 1) + "th visit");
                
                Appointment savedInterview = repository.save(slot);
                
                // Push real-time notification to Admin's Bell Icon
                String positionName = slot.getPurpose().contains(":") ? slot.getPurpose().split(":")[1] : "Interview";
                sendAdminNotification("🚨 NEW INTERVIEW: " + savedInterview.getVisitorName() + " for " + positionName, savedInterview);
                
                return savedInterview;
            }

            // CASE B: Standard logic for Admin creating empty time slots
            slot.setAvailable(true);
            return repository.save(slot);
        } catch (Exception e) {
            System.err.println("Error creating slot: " + e.getMessage());
            return null;
        }
    }
    
    // --- NEW ENDPOINT REQUIRED BY YOUR REACT COMPONENT ---
    // This allows ManageSlots.js to fetch items specifically for the dashboard layout
    @GetMapping("/interviews")
    public List<Appointment> getInterviews() {
        return repository.findAll().stream()
                .filter(app -> app.getPurpose() != null && app.getPurpose().toUpperCase().contains("INTERVIEW"))
                .toList();
    }
    
    @GetMapping("/available")
    public List<Appointment> getAvailable() {
        List<Appointment> allAvailable = repository.findByAvailableTrue();
        
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

        return allAvailable.stream().filter(slot -> {
            try {
                LocalDate slotDate = LocalDate.parse(slot.getDate());
                String timeStr = slot.getTimeIn().trim().toUpperCase();
                LocalTime slotTime = LocalTime.parse(timeStr, tf);
                
                if (slotDate.isAfter(today)) return true;
                if (slotDate.isEqual(today)) return slotTime.isAfter(now);
                
                return false;
            } catch (Exception e) {
                System.err.println("Error parsing slot: " + slot.getTimeIn());
                return false;
            }
        }).toList();
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Appointment> getAppointmentDetail(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/booked")
    public List<Appointment> getBookedAppointments() {
        // findByAvailableFalse = direct MongoDB query, much faster than findAll + filter
        return repository.findByAvailableFalse().stream()
                .map(this::stripHeavyFields)
                .toList();
    }
    
    @GetMapping("/all")
    public List<Appointment> getAllAppointments() {
        // Also use findByAvailableFalse — /all is only used for history, empty slots not needed
        return repository.findByAvailableFalse().stream()
                .map(this::stripHeavyFields)
                .toList();
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
            slot.setResumeFile(data.getResumeFile());
            slot.setGender(data.getGender());
            slot.setIdProof(data.getIdProof());
            slot.setReference(data.getReference());
            slot.setRemark(data.getRemark());
            slot.setAvailable(false);
            slot.setStatus("Pending");

            Appointment saved = repository.save(slot);
            String msg = "New Request from " + saved.getVisitorName();
            if (saved.getPurpose() != null && saved.getPurpose().toUpperCase().contains("INTERVIEW")) {
                msg = "🚨 INTERVIEW SCHEDULED: " + saved.getVisitorName();
            }
            
            sendAdminNotification(msg, saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Slot not found"));
    }

    // --- RESCHEDULE LOGIC ---
    @PutMapping("/reschedule/{oldId}")
    public ResponseEntity<Appointment> reschedule(@PathVariable String oldId, @RequestBody Map<String, Object> payload) {
        return repository.findById(oldId).map(oldSlot -> {
            String newSlotId = (String) payload.get("newSlotId");

            if (newSlotId == null || newSlotId.equals(oldId)) {
                updateSlotFields(oldSlot, payload);
                Appointment saved = repository.save(oldSlot);
                sendAdminNotification("Details Updated by " + saved.getVisitorName(), saved);
                return ResponseEntity.ok(saved);
            }

            return repository.findById(newSlotId).map(newSlot -> {
                updateSlotFields(newSlot, payload);
                newSlot.setStatus("Pending");
                newSlot.setAvailable(false);
                repository.save(newSlot);

                sendAdminNotification("Meeting Rescheduled by " + newSlot.getVisitorName(), newSlot);

                clearSlotFields(oldSlot);
                repository.save(oldSlot);

                return ResponseEntity.ok(newSlot);
            }).orElse(ResponseEntity.status(404).build());
        }).orElse(ResponseEntity.status(404).build());
    }

    // --- USER SIDE: CANCEL (FREES UP THE SLOT) ---
    @PutMapping("/cancel/{id}") 
    public ResponseEntity<?> deleteAppointment(@PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            String vName = appointment.getVisitorName();
            String vPurpose = appointment.getPurpose();

            clearSlotFields(appointment); 
            repository.save(appointment); 
            
            if (vName != null) {
                String customMessage = "Meeting is cancelled by " + vName + " for " + vPurpose;
                sendAdminNotification(customMessage, appointment);
            }
            
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
    
    // --- ADMIN SIDE: DELETE (REMOVES THE SLOT) ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            repository.deleteById(id);
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
        return ResponseEntity.ok(
            repository.findByEmail(email).stream()
                .map(this::stripHeavyFields)
                .toList()
        );
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<Appointment> updateStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        return repository.findById(id).map(appointment -> {
            String newStatus = payload.get("status");
            
            if ("Approved".equalsIgnoreCase(newStatus) && appointment.getTokenNumber() == null) {
                List<Appointment> todaysBookings = repository.findByDateAndStatus(appointment.getDate(), "Approved");
                int nextQueueIdx = todaysBookings.size() + 1;
                
                appointment.setQueuePosition(nextQueueIdx);
                appointment.setTokenNumber("TK-" + appointment.getDate().replace("-", "") + "-" + String.format("%03d", nextQueueIdx));
                appointment.setReminderSent(false);
            }

            appointment.setStatus(newStatus);
            Appointment saved = repository.save(appointment);
            
            emailService.sendVisitorStatusEmail(saved);

            String userMessage = "";
            if ("Approved".equalsIgnoreCase(newStatus)) {
                userMessage = "Meeting Approved! Please arrive at " + saved.getTimeIn() + 
                              " on " + saved.getDate() + ". Kindly reach 5 mins before time.";
            } else if ("Declined".equalsIgnoreCase(newStatus)) {
                userMessage = "Sorry " + saved.getVisitorName() + ", your meeting for " + saved.getDate() + " is cancelled. Please select another time slot to meet.";
            } else if ("Visited".equalsIgnoreCase(newStatus)) {
                userMessage = "Thank you for visiting us, " + saved.getVisitorName() + "! It was a pleasure meeting you. Have a great day ahead!";
            } else if ("Not Visited".equalsIgnoreCase(newStatus)) {
                userMessage = "You missed your appointment scheduled for " + saved.getTimeIn() + ". If you still need to meet, please reschedule a new slot.";
            }

            if (!userMessage.isEmpty()) {
                Notification userNotifDb = new Notification();
                userNotifDb.setRecipient(saved.getEmail().toLowerCase().trim());
                userNotifDb.setMessage(userMessage);
                userNotifDb.setStatus(newStatus);

                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH);
                userNotifDb.setReceivedAt(now.format(formatter));
                userNotifDb.setRead(false);
                notificationRepository.save(userNotifDb);

                String destination = "/topic/user-" + saved.getEmail().toLowerCase().trim();
                messagingTemplate.convertAndSend(destination, userNotifDb);
            }

            sendAdminNotification("Status updated to " + newStatus + " for " + saved.getVisitorName(), saved);

            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Strip visitorPhoto and resumeFile from list responses — never lose the real ID
    private Appointment stripHeavyFields(Appointment a) {
        boolean hasResume = a.getResumeFile() != null && !a.getResumeFile().isEmpty();
        a.setHasResumeFile(hasResume);
        a.setVisitorPhoto(null);
        a.setResumeFile(null);
        return a;
    }

    public void sendConfirmationNotification(Appointment app) {
        String msg = "✅ Appointment Confirmed via Email: " + app.getVisitorName();
        sendAdminNotification(msg, app);
    }

    public void sendCancellationNotification(Appointment app) {
        String msg = "❌ Appointment Cancelled via Email: " + app.getVisitorName();
        sendAdminNotification(msg, app);
    }
    
    @GetMapping("/user-stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@RequestParam String email) {
        try {
            List<Appointment> userMeetings = repository.findByEmail(email);
            Map<String, Object> stats = new HashMap<>();
            
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

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

            long pastCount = userMeetings.stream()
                    .filter(m -> "Visited".equalsIgnoreCase(m.getStatus()) || 
                                 "Completed".equalsIgnoreCase(m.getStatus()))
                    .count();

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