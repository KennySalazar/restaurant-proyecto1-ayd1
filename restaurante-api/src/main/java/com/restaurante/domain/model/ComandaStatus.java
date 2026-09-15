package com.restaurante.domain.model;

/**
 * Estados del ciclo de vida de una comanda en el restaurante.
 */
public enum ComandaStatus {
    BORRADOR,
    RECIBIDA,
    EN_PREPARACION,
    LISTA,
    ENTREGADA,
    CANCELADA
}
