package com.restaurante.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ChallengeResponse(
        UUID challengeId,
        Instant expiresAt,
        String message
) {
}
