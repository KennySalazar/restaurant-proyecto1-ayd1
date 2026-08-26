package com.restaurante.application.auth;

import com.restaurante.application.mail.EmailService;
import com.restaurante.domain.model.OtpPurpose;
import com.restaurante.domain.model.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpDeliveryService {

    private final OtpService otpService;
    private final EmailService emailService;

    public OtpDeliveryService(
            OtpService otpService,
            EmailService emailService
    ) {
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Transactional
    public OtpIssue send(
            UserAccount user,
            OtpPurpose purpose
    ) {
        OtpIssue issue = otpService.issue(user, purpose);

        emailService.sendOtp(
                user.getEmail(),
                issue.code(),
                purpose,
                otpService.expirationMinutes()
        );

        return issue;
    }
}
