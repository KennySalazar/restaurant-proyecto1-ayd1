package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumen consolidado de alertas de inventario bajo")
public record SupplyAlertSummaryResponse(
                @Schema(description = "Cantidad total de insumos con alerta activa", example = "3") int totalAlerts,

                @Schema(description = "Cantidad de insumos agotados (stock en cero o negativo)", example = "1") int outOfStockCount,

                @Schema(description = "Cantidad de insumos con stock bajo (mayor a cero pero menor o igual al mínimo)", example = "2") int lowStockCount,

                @Schema(description = "Listado detallado de alertas activas") List<SupplyAlertResponse> alerts) {
}
