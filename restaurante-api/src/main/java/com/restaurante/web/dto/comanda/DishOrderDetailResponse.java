package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle del platillo registrado en la cuenta.
 */
@Schema(description = "Detalle del platillo ordenado en la cuenta")
public record DishOrderDetailResponse(
        @Schema(description = "Identificador único del detalle de comanda", example = "15")
        Long detalleId,

        @Schema(description = "Identificador del platillo", example = "1")
        Long platilloId,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String nombrePlatillo,

        @Schema(description = "Cantidad ordenada", example = "2")
        short cantidad,

        @Schema(description = "Precio unitario base", example = "45.00")
        BigDecimal precioUnitario,

        @Schema(description = "Subtotal (incluyendo modificadores)", example = "90.00")
        BigDecimal subtotal,

        @Schema(description = "Notas especiales de preparación", example = "Término medio")
        String notas,

        @Schema(description = "Nombres de los modificadores aplicados", example = "[\"Queso extra\", \"Tocineta\"]")
        List<String> modificadores,

        @Schema(description = "Estado del platillo", example = "BORRADOR")
        String estado
) {
}
