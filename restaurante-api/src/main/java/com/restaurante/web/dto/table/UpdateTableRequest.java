package com.restaurante.web.dto.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateTableRequest(

        @NotBlank
        @Size(max = 20)
        String numero,

        @NotNull
        @Positive
        Short capacidad,

        @NotNull
        Long zonaId
) {
}