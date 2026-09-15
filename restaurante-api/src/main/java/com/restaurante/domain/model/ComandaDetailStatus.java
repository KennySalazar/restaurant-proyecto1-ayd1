package com.restaurante.domain.model;

/**
 * Estados del ciclo de vida de un detalle de comanda (platillo o combo).
 */
public enum ComandaDetailStatus {
    BORRADOR,
    RECIBIDO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,
    NO_DISPONIBLE,
    CANCELADO
}
