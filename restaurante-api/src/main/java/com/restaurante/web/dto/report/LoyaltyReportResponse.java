package com.restaurante.web.dto.report;

import java.time.LocalDate;
import java.util.List;

public record LoyaltyReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Long puntosOtorgados,
        Long puntosRedimidos,
        List<LoyaltyFrequentCustomerResponse> clientesFrecuentes
) {
}