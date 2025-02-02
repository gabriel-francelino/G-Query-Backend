package com.elasticsearch.search.controller;

import com.elasticsearch.search.domain.EmailRequestDto;
import com.elasticsearch.search.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/send-email")
    public ResponseEntity<String> sendEmail() {
        emailService.sendEmail();
        return ResponseEntity.ok("Email sent successfully!");
    }

    @PostMapping("/search/send-email")
    public ResponseEntity<String> sendDocsByEmail(@RequestBody EmailRequestDto emailRequest) {
        try {
            emailService.sendDocumentByEmail(emailRequest);
            return ResponseEntity.ok("Email sent successfully!");
        } catch (MessagingException e) {
            return ResponseEntity.badRequest().body("Error sending email: " + e.getMessage());
        }
    }
}
