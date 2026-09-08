package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendOtp(
            String recipient,
            String code,
            OtpPurpose purpose,
            int expirationMinutes
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }

        message.setTo(recipient);
        message.setSubject(OtpEmailContent.subjectFor(purpose));
        message.setText(OtpEmailContent.bodyFor(code, purpose, expirationMinutes));

        mailSender.send(message);
    }

}
