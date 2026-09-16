package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record SalesByWaiterResponse(
        Long meseroId,
        String mesero,
        Long cantidadVentas,
        BigDecimal montoVendido
) {
}