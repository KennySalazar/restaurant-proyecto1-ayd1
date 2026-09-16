package com.restaurante.web.dto.cash;

public record CashRegisterAvailabilityResponse(
        Long cajaId,
        String codigo,
        String nombre,
        String ubicacion,
        boolean activa,
        boolean disponible
) {
}