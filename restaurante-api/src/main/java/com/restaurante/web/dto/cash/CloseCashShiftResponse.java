package com.restaurante.web.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CloseCashShiftResponse(
        Long id,
        Long cajaId,
        Long cajeroId,
        String estado,
        BigDecimal montoInicialEfectivo,
        BigDecimal efectivoEsperado,
        BigDecimal efectivoReal,
        BigDecimal diferencia,
        Instant abiertaEn,
        Instant cerradaEn,
        String observaciones
) {
}