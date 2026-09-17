package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record WaiterPerformanceResponse(
        Long meseroId,
        String mesero,
        Long cantidadVentas,
        BigDecimal montoTotalVendido,
        Long cantidadCalificaciones,
        BigDecimal calificacionPromedio
) {
}