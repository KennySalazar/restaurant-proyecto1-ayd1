package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Detalle de una alerta de inventario bajo para un insumo")
public record SupplyAlertResponse(
                @Schema(description = "Identificador único del insumo", example = "1") Long supplyId,

                @Schema(description = "Código del insumo", example = "INS-0001") String supplyCode,

                @Schema(description = "Nombre del insumo", example = "Harina de Trigo") String supplyName,

                @Schema(description = "Identificador de la categoría", example = "4") Long categoryId,

                @Schema(description = "Nombre de la categoría", example = "Abarrote") String categoryName,

                @Schema(description = "Identificador de la unidad de medida", example = "2") Short measurementUnitId,

                @Schema(description = "Nombre de la unidad de medida", example = "Kilogramo") String measurementUnitName,

                @Schema(description = "Abreviatura de la unidad de medida", example = "kg") String measurementUnitAbbreviation,

                @Schema(description = "Existencia actual en inventario", example = "2.0000") BigDecimal currentStock,

                @Schema(description = "Nivel de stock mínimo configurado", example = "10.0000") BigDecimal minimumStock,

                @Schema(description = "Capacidad máxima de almacenamiento", example = "100.0000") BigDecimal maximumStock,

                @Schema(description = "Déficit respecto al stock mínimo requerido", example = "8.0000") BigDecimal deficit,

                @Schema(description = "Nivel de severidad de la alerta", example = "BAJO", allowableValues = {
                                "BAJO", "AGOTADO" }) String alertLevel,

                @Schema(description = "Nivel de prioridad de la notificación", example = "ALTA", allowableValues = {
                                "ALTA", "CRITICA" }) String priority,

                @Schema(description = "Mensaje descriptivo de la alerta", example = "El insumo Harina de Trigo alcanzó el nivel mínimo de stock") String message,

                @Schema(description = "Fecha y hora de generación o detección de la alerta", example = "2026-09-13T20:00:00Z") Instant timestamp) {
}
