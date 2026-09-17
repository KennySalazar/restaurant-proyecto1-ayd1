package com.restaurante.web.dto.report;

import java.time.LocalDate;
import java.util.List;

public record HistoricalProfitabilityReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean huboVentas,
        List<HistoricalDishProfitabilityResponse> platillos
) {
}