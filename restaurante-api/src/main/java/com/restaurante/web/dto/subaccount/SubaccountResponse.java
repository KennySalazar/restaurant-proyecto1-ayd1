package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Representación de una sub-cuenta generada a partir de la división de una cuenta.
 */
@Schema(description = "Detalle de una subcuenta")
public record SubaccountResponse(
        @Schema(description = "Identificador único de la subcuenta", example = "1")
        Long id,

        @Schema(description = "Identificador de la cuenta principal", example = "10")
        Long cuentaId,

        @Schema(description = "Número correlativo de la subcuenta", example = "1")
        short numeroSubcuenta,

        @Schema(description = "Nombre asignado a la subcuenta", example = "Persona 1")
        String nombre,

        @Schema(description = "Tipo de división ('PERSONAS' o 'ITEMS')", example = "PERSONAS")
        String tipoDivision,

        @Schema(description = "Porcentaje asignado de la cuenta (en división por personas)", example = "33.3333")
        BigDecimal porcentajeAsignado,

        @Schema(description = "Estado de la subcuenta ('PENDIENTE', 'FACTURADA', 'CANCELADA')", example = "PENDIENTE")
        String estado,

        @Schema(description = "Subtotal calculado correspondiente a la subcuenta", example = "120.00")
        BigDecimal subtotal,

        @Schema(description = "Fecha de creación de la subcuenta")
        Instant creadoEn,

        @Schema(description = "Listado de platillos/ítems asignados a la subcuenta")
        List<SubaccountItemResponse> items
) {
}
