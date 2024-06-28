package com.elasticsearch.search.service;

import com.elasticsearch.search.domain.EmailRequestDto;
import com.elasticsearch.search.utils.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remetente);
        message.setTo("gabrielfrancelino3c@gmail.com");
        message.setSubject("Teste de envio de email");
        message.setText("Teste de envio de email com o Spring Boot");
        mailSender.send(message);
    }

    public void sendDocumentByEmail(EmailRequestDto emailRequest) {
        SimpleMailMessage message = new SimpleMailMessage();

        String body = Util.generateEmailBody(emailRequest.results());

        message.setFrom(remetente);
        message.setTo("gabrielfrancelino3c@gmail.com");
        message.setCc("gabriel.piva@sou.unifal-mg.edu.br");
        message.setSubject("Confira os Resultados da Sua Última Busca \uD83D\uDD0D");
        message.setText(body);
        mailSender.send(message);
    }
}
