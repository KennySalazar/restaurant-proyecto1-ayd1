package com.restaurante.application.common;

import com.restaurante.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_email", "Solicitud inválida",
                "El correo electrónico es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
