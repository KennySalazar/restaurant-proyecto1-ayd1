package com.restaurante.web.dto.reservation;

import com.restaurante.domain.model.ReservationStatus;

import java.time.OffsetDateTime;

public record ReservationResponse(
        Long id,
        String codigoReserva,
        String nombreCliente,
        String telefonoCliente,
        Short cantidadPersonas,
        OffsetDateTime fechaHoraInicio,
        OffsetDateTime fechaHoraFin,
        ReservationStatus estado,
        String notas,
        ReservationTableResponse mesa
) {
}