package com.restaurante.web.dto.report;

import java.time.LocalDate;

public record TableOccupancySlotResponse(
        LocalDate fecha,
        Integer hora,
        Long mesasOcupadas
) {
}