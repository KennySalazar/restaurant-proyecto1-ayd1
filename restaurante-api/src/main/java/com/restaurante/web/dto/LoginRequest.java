package com.restaurante.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Schema(example = "persona@ejemplo.com")
    String email,
    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {
}
