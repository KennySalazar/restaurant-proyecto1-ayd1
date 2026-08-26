package com.restaurante.application.auth;

import java.time.Instant;
import java.util.UUID;

public record OtpIssue(
        UUID challengeId,
        String code,
        Instant expiresAt
) {
}
