package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de alerta de inventario bajo de un insumo específico")
public record SingleSupplyAlertStatusResponse(
                @Schema(description = "Indica si el insumo tiene una alerta activa de stock bajo", example = "true") boolean hasAlert,

                @Schema(description = "Detalle de la alerta activa, o null si el stock es suficiente") SupplyAlertResponse alert) {
}
