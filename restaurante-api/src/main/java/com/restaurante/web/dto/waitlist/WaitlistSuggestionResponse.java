package com.restaurante.web.dto.waitlist;

import com.restaurante.domain.model.WaitlistStatus;

import java.time.OffsetDateTime;

public record WaitlistSuggestionResponse(
        Long id,
        Integer posicion,
        String nombreCliente,
        String telefonoCliente,
        Short cantidadPersonas,
        OffsetDateTime horaLlegada,
        WaitlistStatus estado,
        Long mesaId,
        String numeroMesa,
        Short capacidadMesa
) {
}