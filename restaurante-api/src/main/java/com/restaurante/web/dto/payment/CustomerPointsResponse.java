package com.restaurante.web.dto.payment;

import java.math.BigDecimal;

public record CustomerPointsResponse(
        Long clienteId,
        String nombres,
        String apellidos,
        Long saldoPuntos,
        BigDecimal valorMonetarioPunto,
        BigDecimal valorMonetarioDisponible
) {
}