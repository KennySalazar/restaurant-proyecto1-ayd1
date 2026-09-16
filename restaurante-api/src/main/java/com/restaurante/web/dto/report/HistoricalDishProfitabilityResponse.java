package com.restaurante.web.dto.report;

import java.math.BigDecimal;

public record HistoricalDishProfitabilityResponse(
        Long platilloId,
        String platillo,
        BigDecimal cantidadVendida,
        BigDecimal ingresoHistorico,
        BigDecimal costoHistorico,
        BigDecimal gananciaHistorica,
        BigDecimal margenRentabilidad
) {
}