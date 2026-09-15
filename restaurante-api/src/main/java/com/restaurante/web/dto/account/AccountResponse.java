package com.restaurante.web.dto.account;

import com.restaurante.domain.model.TableStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Detalle y confirmación tras la apertura de una cuenta en una mesa.
 */
@Schema(description = "Detalle de la cuenta y estado de la mesa")
public record AccountResponse(
        @Schema(description = "Identificador único de la cuenta", example = "15")
        Long id,

        @Schema(description = "Identificador del restaurante", example = "1")
        Long restauranteId,

        @Schema(description = "Identificador de la mesa asociada", example = "3")
        Long mesaId,

        @Schema(description = "Número de la mesa", example = "M-03")
        String numeroMesa,

        @Schema(description = "Identificador del mesero que abrió la cuenta", example = "5")
        Long meseroId,

        @Schema(description = "Nombre completo del mesero responsable", example = "Juan Pérez")
        String nombreMesero,

        @Schema(description = "Identificador del cliente registrado (si aplica)", example = "10")
        Long clienteId,

        @Schema(description = "Identificador de la reserva asociada (si aplica)", example = "2")
        Long reservaId,

        @Schema(description = "Identificador de la lista de espera asociada (si aplica)", example = "4")
        Long listaEsperaId,

        @Schema(description = "Número único de la cuenta", example = "CTA-M-03-12345")
        String numeroCuenta,

        @Schema(description = "Cantidad de personas en la mesa", example = "4")
        short cantidadPersonas,

        @Schema(description = "Estado actual de la cuenta", example = "ABIERTA")
        String estadoCuenta,

        @Schema(description = "Fecha y hora exacta de apertura de la cuenta")
        Instant abiertaEn,

        @Schema(description = "Observaciones de la cuenta", example = "Cliente solicita mesa junto a la ventana")
        String observaciones,

        @Schema(description = "Estado de la mesa tras la apertura", example = "OCUPADA")
        TableStatus estadoMesa
) {
}
