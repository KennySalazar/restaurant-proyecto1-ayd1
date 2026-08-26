package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
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
        message.setSubject(subjectFor(purpose));
        message.setText(bodyFor(code, purpose, expirationMinutes));

        mailSender.send(message);
    }

    private String subjectFor(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN ->
                    "Código de inicio de sesión";
            case PASSWORD_RECOVERY ->
                    "Recuperación de contraseña";
            case TWO_FACTOR_ENABLE ->
                    "Activación de autenticación de dos factores";
            case TWO_FACTOR_DISABLE ->
                    "Desactivación de autenticación de dos factores";
        };
    }

    private String bodyFor(
            String code,
            OtpPurpose purpose,
            int expirationMinutes
    ) {
        String action = switch (purpose) {
            case LOGIN ->
                    "completar tu inicio de sesión";
            case PASSWORD_RECOVERY ->
                    "recuperar tu contraseña";
            case TWO_FACTOR_ENABLE ->
                    "activar la autenticación de dos factores";
            case TWO_FACTOR_DISABLE ->
                    "desactivar la autenticación de dos factores";
        };

        return """
                Sistema de Gestión de Restaurante

                Utiliza el siguiente código para %s:

                %s

                Este código expira en %d minutos.

                Si no solicitaste esta operación, puedes ignorar este mensaje.
                """.formatted(action, code, expirationMinutes);
    }
}
