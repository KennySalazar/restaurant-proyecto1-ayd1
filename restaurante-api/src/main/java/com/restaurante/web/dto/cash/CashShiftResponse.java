package com.restaurante.web.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CashShiftResponse(
        Long id,
        Long cajaId,
        String codigoCaja,
        String nombreCaja,
        Long cajeroId,
        String estado,
        BigDecimal montoInicialEfectivo,
        Instant abiertaEn,
        String observaciones
) {
}