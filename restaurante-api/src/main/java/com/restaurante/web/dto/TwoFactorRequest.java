package com.restaurante.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorRequest(

        @NotBlank
        String currentPassword

) {
}
