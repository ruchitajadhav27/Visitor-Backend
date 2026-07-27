package com.visitor.Visitor_Backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Document(collection = "notifications")
@Data
public class Notification {
    @Id
    private String id;
    private String recipient; // Use "ADMIN" for admin, or the user's email
    private String message;
    private String visitorName;
    private String purpose;
    private String dateTime;
    private String status;
    private String receivedAt; // e.g., "10:30 AM"
    
    @Field("isRead") // Forces MongoDB to use this name
    @JsonProperty("isRead") // Forces JSON to use this name
    private boolean isRead = false;
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public boolean isRead() {
		return isRead;
	}
	public void setRead(boolean isRead) {
		this.isRead = isRead;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReceivedAt() {
		return receivedAt;
	}
	public void setReceivedAt(String receivedAt) {
		this.receivedAt = receivedAt;
	}
	public String getVisitorName() {
		return visitorName;
	}
	public void setVisitorName(String visitorName) {
		this.visitorName = visitorName;
	}
}
