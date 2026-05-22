package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.SubscriptionToken;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.InitializingBean; // <-- Naya built-in import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
public class WebPushService implements InitializingBean { // <-- Interface implement kiya

    @Value("${vapid.public.key}")
    private String publicKey;

    @Value("${vapid.private.key}")
    private String privateKey;

    @Value("${vapid.subject}")
    private String subject;

    private PushService pushService;

    // @PostConstruct ko poori tarah hata diya, ab yeh method automatic chalega
    @Override
    public void afterPropertiesSet() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            pushService = new PushService(publicKey, privateKey, subject);
            System.out.println("✅ VAPID Keys initialized successfully using InitializingBean!");
        } catch (Exception e) {
            System.err.println("VAPID Keys initialization failed: " + e.getMessage());
        }
    }

    public void sendPushNotification(String endpoint, String p256dh, String auth, String messageJson) {
        try {
            Notification notification = new Notification(
                endpoint,
                p256dh,
                auth,
                messageJson
            );

            pushService.send(notification);
            System.out.println("Push notification successfully sent via WNS/Google!");
        } catch (Exception e) {
            System.err.println("Failed to send web push: " + e.getMessage());
        }
    }
}