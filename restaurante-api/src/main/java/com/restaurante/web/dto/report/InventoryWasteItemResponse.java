package com.restaurante.web.dto.report;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryWasteItemResponse(
        String fuente,
        Long mermaId,
        Long cancelacionComandaDetalleId,
        String numeroDocumento,
        Instant fechaRegistro,
        Long insumoId,
        String insumo,
        String categoria,
        BigDecimal cantidad,
        String unidadMedida,
        String tipoMotivo,
        String motivoGeneral,
        BigDecimal costoUnitario,
        BigDecimal costoTotal
) {
}
