package com.restaurante.web.dto;

import com.restaurante.web.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "La contraseña actual es obligatoria")
    String currentPassword,
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @ValidPassword
    String newPassword
) {
}
