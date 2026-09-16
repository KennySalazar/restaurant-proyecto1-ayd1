package com.restaurante.web.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ChargeRequest(

        @PositiveOrZero
        Long puntosRedimidos,

        @NotEmpty
        List<@Valid RegisterPaymentRequest> pagos
) {
}