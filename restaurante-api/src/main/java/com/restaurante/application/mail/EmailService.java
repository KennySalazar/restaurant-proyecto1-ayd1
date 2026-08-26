package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;

public interface EmailService {

    void sendOtp(
            String recipient,
            String code,
            OtpPurpose purpose,
            int expirationMinutes
    );
}
