package com.restaurante.web.dto.reservation;

public record ReservationTableResponse(
        Long id,
        String numero,
        Short capacidad,
        String zona
) {
}