package com.visitor.Visitor_Backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subscription_tokens")
public class SubscriptionToken {
    @Id
    private String id;
    private String email;
    private String endpoint;
    private String p256dh;
    private String auth;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }
    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }
}