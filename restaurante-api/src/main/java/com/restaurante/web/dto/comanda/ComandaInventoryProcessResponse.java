package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta de confirmación tras procesar el envío de una comanda y descontar su inventario.
 */
@Schema(description = "Respuesta de confirmación del procesamiento de comanda y descuento de inventario")
public record ComandaInventoryProcessResponse(
        @Schema(description = "Mensaje de confirmación", example = "Procesamiento de comanda confirmado exitosamente")
        String message,

        @Schema(description = "Datos consolidados de la comanda procesada")
        ComandaResponse comanda,

        @Schema(description = "Movimientos de salida registrados en el kardex de inventario")
        List<KardexMovementResponse> movements,

        @Schema(description = "Platillos rechazados por falta de stock de insumos")
        List<RejectedDishDetailResponse> rejectedDishes
) {
    public ComandaInventoryProcessResponse(String message, ComandaResponse comanda, List<KardexMovementResponse> movements) {
        this(message, comanda, movements, List.of());
    }
}
