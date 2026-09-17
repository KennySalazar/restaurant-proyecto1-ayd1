package com.restaurante.web.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryWasteSectionResponse(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean seccionGenerada,
        boolean periodoValido,
        String mensajePeriodo,
        Long totalRegistros,
        BigDecimal cantidadTotal,
        BigDecimal costoTotal,
        List<InventoryWasteItemResponse> mermas
) {
}
