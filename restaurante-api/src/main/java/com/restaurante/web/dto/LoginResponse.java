package com.restaurante.web.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean requiresTwoFactor,
        UUID challengeId,
        Instant challengeExpiresAt,
        String message
) {

    public static LoginResponse token(
            String accessToken,
            long expiresIn
    ) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                false,
                null,
                null,
                "Autenticación exitosa"
        );
    }

    public static LoginResponse challenge(
            UUID challengeId,
            Instant expiresAt
    ) {
        return new LoginResponse(
                null,
                null,
                0,
                true,
                challengeId,
                expiresAt,
                "Se envió un código de verificación al correo electrónico"
        );
    }
}
