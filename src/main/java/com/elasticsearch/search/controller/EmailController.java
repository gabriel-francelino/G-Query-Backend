package com.elasticsearch.search.controller;

import com.elasticsearch.search.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
