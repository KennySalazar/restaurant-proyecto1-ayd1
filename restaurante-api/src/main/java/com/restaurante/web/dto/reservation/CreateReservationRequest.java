package com.restaurante.web.dto.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateReservationRequest(

        @NotNull
        Long mesaId,

        @NotBlank
        @Size(max = 150)
        String nombreCliente,

        @NotBlank
        @Size(max = 25)
        String telefonoCliente,

        @NotNull
        @Min(1)
        Short cantidadPersonas,

        @NotNull
        @Future
        OffsetDateTime fechaHoraInicio,

        @Size(max = 500)
        String notas
) {
}