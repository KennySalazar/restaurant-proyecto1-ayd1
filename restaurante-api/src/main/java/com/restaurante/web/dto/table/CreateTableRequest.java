package com.restaurante.web.dto.table;

import com.restaurante.domain.model.TableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(

        @NotBlank
        @Size(max = 20)
        String numero,

        @NotNull
        @Positive
        Short capacidad,

        @NotNull
        Long zonaId,

        @NotNull
        TableStatus estadoInicial
) {
}