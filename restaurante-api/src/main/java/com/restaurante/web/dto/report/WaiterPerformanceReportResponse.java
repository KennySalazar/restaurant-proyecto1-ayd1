package com.restaurante.web.dto.report;

import java.time.LocalDate;
import java.util.List;

public record WaiterPerformanceReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean huboDatos,
        List<WaiterPerformanceResponse> meseros
) {
}