package com.restaurante.web.dto.report;

public record LoyaltyFrequentCustomerResponse(
        Long clienteId,
        String nombres,
        String apellidos,
        Long cantidadVisitas
) {
}