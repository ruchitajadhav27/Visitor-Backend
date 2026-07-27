package com.visitor.Visitor_Backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "appointments")
public class Appointment {
    @Id
    private String id;
    private String date;      
    private String day;       
    private String timeIn;    
    private String timeOut;   
    private boolean available = true;
    private String visitorName;
    private String phone;
    private String email;
    private String address;
    private String gender;
    private String purpose;
    private String description;
    private String whomToMeet;
    private String idProof;
    private String reference;
    private String visitorPhoto; 
    private String remark;
    private String status = "Pending";
    private String resumeFile; // Base64 PDF string
    private String visitHistory;
    
    private String tokenNumber; 
    private Integer queuePosition;
    private boolean reminderSent = false;
    private transient boolean hasResumeFile = false; // set in list responses only, never stored in DB

    // --- MANUAL GETTERS AND SETTERS (Fixes the errors) ---
    
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public boolean isReminderSent() { return reminderSent; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getTimeIn() { return timeIn; }
    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }

    public String getTimeOut() { return timeOut; }
    public void setTimeOut(String timeOut) { this.timeOut = timeOut; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getVisitorName() { return visitorName; }
    public void setVisitorName(String visitorName) { this.visitorName = visitorName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWhomToMeet() { return whomToMeet; }
    public void setWhomToMeet(String whomToMeet) { this.whomToMeet = whomToMeet; }

    public String getVisitorPhoto() { return visitorPhoto; }
    public void setVisitorPhoto(String visitorPhoto) { this.visitorPhoto = visitorPhoto; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
	public String getIdProof() {
		return idProof;
	}
	public void setIdProof(String idProof) {
		this.idProof = idProof;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	// Add Getters and Setters
	public String getResumeFile() { return resumeFile; }
	public void setResumeFile(String resumeFile) { this.resumeFile = resumeFile; }

	public String getVisitHistory() { return visitHistory; }
	public void setVisitHistory(String visitHistory) { this.visitHistory = visitHistory; }

	public boolean isHasResumeFile() { return hasResumeFile; }
	public void setHasResumeFile(boolean hasResumeFile) { this.hasResumeFile = hasResumeFile; }
}