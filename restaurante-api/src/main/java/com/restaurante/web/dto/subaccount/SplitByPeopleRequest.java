package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Solicitud para dividir una cuenta equitativamente entre un número de personas.
 */
@Schema(description = "Datos para dividir una cuenta abierta entre un número de personas")
public record SplitByPeopleRequest(
        @NotNull(message = "El número de personas es requerido")
        @Min(value = 2, message = "Debe dividirse entre al menos 2 personas")
        @Schema(description = "Número de personas entre las cuales se divide la cuenta", example = "3")
        Integer numeroPersonas,

        @Schema(description = "Nombres opcionales personalizados para cada subcuenta o persona", example = "[\"Juan\", \"Carlos\", \"María\"]")
        List<String> nombres
) {
}
