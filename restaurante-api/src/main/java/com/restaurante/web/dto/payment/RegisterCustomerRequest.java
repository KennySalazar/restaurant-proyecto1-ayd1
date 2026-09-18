package com.restaurante.web.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(

        @NotBlank
        @Size(max = 120)
        String nombres,

        @Size(max = 120)
        String apellidos,

        @NotBlank
        @Size(max = 25)
        String telefono,

        @Email
        @Size(max = 150)
        String correo
) {
}
