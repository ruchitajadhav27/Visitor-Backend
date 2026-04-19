package com.visitor.Visitor_Backend.controller;

import com.visitor.Visitor_Backend.model.Appointment;

import com.visitor.Visitor_Backend.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

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

    @PostMapping("/slots")
    public Appointment createSlot(@RequestBody Appointment slot) {
        try {
            // 1. Calculate Day
            LocalDate localDate = LocalDate.parse(slot.getDate()); // Expects YYYY-MM-DD
            slot.setDay(localDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH));

            // 2. Calculate TimeOut
            // Note: Ensure React sends time as "10:00 AM"
            DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
            LocalTime openTime = LocalTime.parse(slot.getTimeIn().toUpperCase(), tf);
            slot.setTimeOut(openTime.plusMinutes(15).format(tf));
            
            slot.setAvailable(true);
            return repository.save(slot);
        } catch (Exception e) {
            System.out.println("Error processing slot: " + e.getMessage());
            return null; 
        }
    }

    @GetMapping("/available")
    public List<Appointment> getAvailable() {
        return repository.findByAvailableTrue();
    }
    
 // ADMIN: Get all booked appointments (where available is false)
    @GetMapping("/booked")
    public List<Appointment> getBookedAppointments() {
        // You'll need to add findByAvailableFalse() to your Repository
        return repository.findAll().stream()
                .filter(slot -> !slot.isAvailable())
                .toList();
    }
    
    @GetMapping("/my-meetings")
    public ResponseEntity<List<Appointment>> getMyMeetings(@RequestParam String email) {
        try {
            // Fetch only meetings belonging to this email
            List<Appointment> userMeetings = repository.findByEmail(email);
            return ResponseEntity.ok(userMeetings);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
 // 1. DELETE: Removes the appointment from MongoDB
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable String id) {
        return repository.findById(id).map(appointment -> {
            // Make the slot available again before deleting or just set available to true
            appointment.setAvailable(true);
            appointment.setVisitorName(null);
            appointment.setEmail(null);
            // If you want to keep the slot but remove the person:
            repository.save(appointment); 
            
            // OR if you want to delete the record entirely:
            // repository.delete(appointment);
            
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // 2. UPDATE: Updates existing info and slot without creating a new record
    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable String id, @RequestBody Appointment data) {
        return repository.findById(id).map(currentMeeting -> {
            // 1. If the time has changed, find the OLD slot and make it available again
            if (!currentMeeting.getTimeIn().equals(data.getTimeIn()) || !currentMeeting.getDate().equals(data.getDate())) {
                
                // Logic to find the record that matches the OLD time and mark it available
                // Note: This assumes your slots are individual documents in the same collection
                List<Appointment> oldSlots = repository.findAll().stream()
                    .filter(a -> a.getDate().equals(currentMeeting.getDate()) && 
                                 a.getTimeIn().equals(currentMeeting.getTimeIn()))
                    .toList();
                
                for (Appointment old : oldSlots) {
                    old.setAvailable(true);
                    repository.save(old);
                }

                // 2. Mark the NEW slot as unavailable
                // You might need a findByDateAndTimeIn method in your repository
                List<Appointment> newSlots = repository.findAll().stream()
                    .filter(a -> a.getDate().equals(data.getDate()) && 
                                 a.getTimeIn().equals(data.getTimeIn()))
                    .toList();

                for (Appointment n : newSlots) {
                    n.setAvailable(false);
                    repository.save(n);
                }
            }

            // 3. Update the actual meeting details
            currentMeeting.setVisitorName(data.getVisitorName());
            currentMeeting.setPhone(data.getPhone());
            currentMeeting.setEmail(data.getEmail());
            currentMeeting.setPurpose(data.getPurpose());
            currentMeeting.setWhomToMeet(data.getWhomToMeet());
            currentMeeting.setTimeIn(data.getTimeIn());
            currentMeeting.setDate(data.getDate());
            
            return ResponseEntity.ok(repository.save(currentMeeting));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/user-stats")
    public ResponseEntity<?> getUserStats(@RequestParam String email) {
        try {
            // 1. Fetch all appointments linked to this user's email
            List<Appointment> allUserMeetings = repository.findByEmail(email);

            // 2. Count "Scheduled" (Pending or Approved)
            long scheduledCount = allUserMeetings.stream()
                    .filter(a -> "Pending".equalsIgnoreCase(a.getStatus()) || "Approved".equalsIgnoreCase(a.getStatus()))
                    .count();

            // 3. Count "Past Visits" (Declined or explicitly marked as Completed/Archived)
            // Or simply: Total - Scheduled
            long pastCount = allUserMeetings.stream()
                    .filter(a -> "Declined".equalsIgnoreCase(a.getStatus()) || "Completed".equalsIgnoreCase(a.getStatus()))
                    .count();

            // 4. Find the single "Next" meeting (The one closest to today's date)
            Appointment nextMeeting = allUserMeetings.stream()
                    .filter(a -> "Approved".equalsIgnoreCase(a.getStatus()))
                    .sorted((a1, a2) -> a1.getDate().compareTo(a2.getDate()))
                    .findFirst()
                    .orElse(null);

            // Prepare JSON Response
            Map<String, Object> stats = new HashMap<>();
            stats.put("scheduledCount", scheduledCount);
            stats.put("pastCount", pastCount);
            stats.put("nextAppointment", nextMeeting);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error calculating stats: " + e.getMessage());
        }
    }
    
    @PutMapping("/reschedule/{oldId}")
    public ResponseEntity<Appointment> reschedule(@PathVariable String oldId, @RequestBody java.util.Map<String, Object> payload) {
        return repository.findById(oldId).map(oldSlot -> {
            String newSlotId = (String) payload.get("newSlotId");

            // CASE A: User updated details but STAYED in the same time slot
            if (newSlotId == null || newSlotId.equals(oldId)) {
                updateSlotFields(oldSlot, payload);
                return ResponseEntity.ok(repository.save(oldSlot));
            }

            // CASE B: User picked a NEW time slot (Move data)
            return repository.findById(newSlotId).map(newSlot -> {
                updateSlotFields(newSlot, payload);
                newSlot.setStatus("Pending");
                newSlot.setAvailable(false);
                repository.save(newSlot);

                // Clear the old slot so it's available for others
                clearSlotFields(oldSlot);
                repository.save(oldSlot);

                return ResponseEntity.ok(newSlot);
            }).orElse(ResponseEntity.status(404).build());
            
        }).orElse(ResponseEntity.status(404).build());
    }

 // Helper method in AppointmentController.java
    private void updateSlotFields(Appointment slot, java.util.Map<String, Object> payload) {
        slot.setVisitorName((String) payload.get("visitorName"));
        slot.setPhone((String) payload.get("phone"));
        slot.setEmail((String) payload.get("email"));
        slot.setAddress((String) payload.get("address")); // Ensure this is present
        slot.setPurpose((String) payload.get("purpose"));
        slot.setWhomToMeet((String) payload.get("whomToMeet"));
        slot.setVisitorPhoto((String) payload.get("visitorPhoto")); // The image string
        slot.setIdProof((String) payload.get("idProof"));
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
    }
    
 // Add this inside AppointmentController.java
    @GetMapping("/all")
    public List<Appointment> getAllAppointments() {
        return repository.findAll();
    }

    // Also, update your status update method to handle Approve/Reject
    @PutMapping("/status/{id}")
    public ResponseEntity<Appointment> updateStatus(@PathVariable String id, @RequestBody java.util.Map<String, String> payload) {
        return repository.findById(id).map(appointment -> {
            appointment.setStatus(payload.get("status"));
            return ResponseEntity.ok(repository.save(appointment));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/book/{id}")
    public Appointment book(@PathVariable String id, @RequestBody Appointment data) {
        // ADD THIS LOG LINE TO CHECK DATA SIZE
        if (data.getVisitorPhoto() != null) {
            System.out.println("DEBUG: Received photo string length: " + data.getVisitorPhoto().length());
        }

        return repository.findById(id).map(slot -> {
        	// --- VISIT HISTORY LOGIC ---
            List<Appointment> previousVisits = repository.findByEmail(data.getEmail());
            if (previousVisits != null && !previousVisits.isEmpty()) {
                // Sort to find the last visit (assuming list isn't sorted)
                Appointment lastVisit = previousVisits.get(previousVisits.size() - 1);
                int visitCount = previousVisits.size() + 1;
                slot.setVisitHistory(visitCount + "th visit (Last: " + lastVisit.getDate() + " - " + lastVisit.getPurpose() + ")");
            } else {
                slot.setVisitHistory("1st Visit");
            }
            slot.setVisitorName(data.getVisitorName());
            slot.setPhone(data.getPhone());
            slot.setEmail(data.getEmail());
            slot.setAddress(data.getAddress());
            slot.setGender(data.getGender());
            slot.setPurpose(data.getPurpose());
            slot.setDescription(data.getDescription());
            slot.setWhomToMeet(data.getWhomToMeet());
            slot.setVisitorPhoto(data.getVisitorPhoto()); // This is the crucial field
            slot.setRemark(data.getRemark());
            slot.setResumeFile(data.getResumeFile());
            slot.setAvailable(false);
            slot.setStatus("Pending");
            return repository.save(slot);
        }).orElseThrow(() -> new RuntimeException("Slot not found"));
    }
}