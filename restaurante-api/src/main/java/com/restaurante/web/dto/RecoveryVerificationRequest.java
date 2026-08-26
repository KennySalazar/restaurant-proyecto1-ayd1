package com.restaurante.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RecoveryVerificationRequest(

        @NotNull
        UUID challengeId,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String otp,

        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword

) {
}
