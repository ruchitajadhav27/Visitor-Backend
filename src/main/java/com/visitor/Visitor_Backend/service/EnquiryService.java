package com.visitor.Visitor_Backend.service;

import com.visitor.Visitor_Backend.model.Enquiry;
import com.visitor.Visitor_Backend.model.Notification;
import com.visitor.Visitor_Backend.repository.EnquiryRepository;
import com.visitor.Visitor_Backend.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EnquiryService {

    @Autowired
    private EnquiryRepository enquiryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${brevo.api.key}")
    private String BREVO_API_KEY;

    @Value("${brevo.sender.email}")
    private String SENDER_EMAIL;

    public void processEnquiry(Enquiry enquiry) {

        // 1. Save Enquiry to Database
        enquiryRepository.save(enquiry);

        // 2. Send Email to Admin
        sendEmail(enquiry);

        // 3. Create Notification
        Notification notification =
                new Notification();

        notification.setRecipient(
                "ADMIN"
        );

        notification.setMessage(
                "New Enquiry from: "
                        + enquiry.getFirstName()
                        + " "
                        + enquiry.getLastName()
                        + " - Message: "
                        + enquiry.getMessage()
        );

        notification.setPurpose(
                "Visitor Inquiry"
        );

        notification.setStatus(
                "New"
        );

        notification.setRead(false);

        DateTimeFormatter dtFormatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"
                );

        notification.setDateTime(
                LocalDateTime.now()
                        .format(dtFormatter)
        );

        notification.setReceivedAt(
                LocalTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "hh:mm a"
                                )
                        )
        );

        Notification savedNotification =
                notificationRepository
                        .save(notification);

        messagingTemplate.convertAndSend(
                "/topic/admin-notifications",
                savedNotification
        );
    }

    private void sendEmail(
            Enquiry enquiry
    ) {

        try {

            RestTemplate restTemplate =
                    new RestTemplate();

            String url =
                    "https://api.brevo.com/v3/smtp/email";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.set(
                    "api-key",
                    BREVO_API_KEY
            );

            headers.set(
                    "accept",
                    "application/json"
            );

            String emailBody =
                    "You have received a new message from the visitors contact form.<br><br>"

                            + "<b>Name:</b> "
                            + enquiry.getFirstName()
                            + " "
                            + enquiry.getLastName()
                            + "<br>"

                            + "<b>Mobile:</b> "
                            + enquiry.getMobile()
                            + "<br>"

                            + "<b>Email:</b> "
                            + enquiry.getEmail()
                            + "<br>"

                            + "<b>Message:</b> "
                            + enquiry.getMessage()
                            + "<br><br>"

                            + "<b>Submitted at:</b> "
                            + enquiry.getSubmittedAt();

            Map<String, Object> body =
                    new HashMap<>();

            body.put(
                    "sender",
                    Map.of(
                            "name",
                            "Visitor Management",
                            "email",
                            SENDER_EMAIL
                    )
            );

            body.put(
                    "to",
                    new Object[]{
                            Map.of(
                                    "email",
                                    "jadhavruchita27@gmail.com"
                            )
                    }
            );

            body.put(
                    "replyTo",
                    Map.of(
                            "email",
                            enquiry.getEmail()
                    )
            );

            body.put(
                    "subject",
                    "VIMS: New Contact Message"
            );

            body.put(
                    "htmlContent",
                    emailBody
            );

            HttpEntity<Map<String, Object>>
                    entity =
                    new HttpEntity<>(
                            body,
                            headers
                    );

            ResponseEntity<String>
                    response =
                    restTemplate.postForEntity(
                            url,
                            entity,
                            String.class
                    );

            System.out.println(
                    "BREVO SUCCESS: "
                            + response.getBody()
            );

        } catch (Exception e) {

            System.err.println(
                    "Email failed to send: "
                            + e.getMessage()
            );
        }
    }
}