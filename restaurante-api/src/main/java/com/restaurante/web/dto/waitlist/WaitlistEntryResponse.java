package com.restaurante.web.dto.waitlist;

import com.restaurante.domain.model.WaitlistStatus;

import java.time.OffsetDateTime;

public record WaitlistEntryResponse(
        Long id,
        String nombreCliente,
        String telefonoCliente,
        Short cantidadPersonas,
        OffsetDateTime horaLlegada,
        WaitlistStatus estado,
        String notas
) {
}