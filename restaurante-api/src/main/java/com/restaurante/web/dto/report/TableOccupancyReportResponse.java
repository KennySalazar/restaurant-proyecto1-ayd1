package com.restaurante.web.dto.report;

import java.time.LocalDate;
import java.util.List;

public record TableOccupancyReportResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean huboOcupaciones,
        List<TableOccupancySlotResponse> ocupacionPorHorario
) {
}