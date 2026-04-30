package com.visitor.Visitor_Backend.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Model class representing a visitor enquiry.
 * Based on the model structure seen in image_1a5f7a.png.
 */
@Document(collection = "enquiries")
public class Enquiry {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String mobile;
    private String email;
    private String message;
    private LocalDateTime submittedAt = LocalDateTime.now();

    // Default Constructor
    public Enquiry() {
    }

    // Parameterized Constructor
    public Enquiry(String firstName, String lastName, String mobile, String email, String message) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobile = mobile;
        this.email = email;
        this.message = message;
        this.submittedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    // ToString method for easier debugging
    @Override
    public String toString() {
        return "Enquiry{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", submittedAt=" + submittedAt +
                '}';
    }
}