package com.restaurante.web.dto.waitlist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWaitlistEntryRequest(

        @NotBlank
        @Size(max = 150)
        String nombreCliente,

        @NotBlank
        @Size(max = 25)
        String telefonoCliente,

        @NotNull
        @Min(1)
        Short cantidadPersonas,

        @Size(max = 500)
        String notas
) {
}