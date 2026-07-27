package com.visitor.Visitor_Backend.service;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Security;

@Service
public class WebPushService implements InitializingBean {

    @Value("${vapid.public.key}")
    private String publicKey;

    @Value("${vapid.private.key}")
    private String privateKey;

    @Value("${vapid.subject}")
    private String subject;

    private PushService pushService;

    @Override
    public void afterPropertiesSet() {

        try {

            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            pushService = new PushService();

            pushService.setSubject(subject);
            pushService.setPublicKey(publicKey);
            pushService.setPrivateKey(privateKey);

            System.out.println("✅ VAPID initialized");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPushNotification(
            String endpoint,
            String p256dh,
            String auth,
            String messageJson) {

        try {

            System.out.println("\n=========== PUSH START ===========");
            System.out.println("Endpoint: " + endpoint);
            System.out.println("Payload: " + messageJson);

            Notification notification =
                    new Notification(
                            endpoint,
                            p256dh,
                            auth,
                            messageJson.getBytes()
                    );

            // IMPORTANT
            pushService.send(notification);

            System.out.println("✅ PUSH SENT SUCCESSFULLY");
            System.out.println("==================================");

        } catch (Exception e) {

            System.out.println("❌ PUSH FAILED");
            e.printStackTrace();
        }
    }
}