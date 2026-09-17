package com.restaurante.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Representación del cálculo de retraso y alerta por tiempo de preparación de un platillo.
 */
public record DishPreparationDelayInfo(
        short estimatedTimeMinutes,
        Instant startTime,
        Instant deadline,
        long elapsedMinutes,
        boolean timeExceeded,
        long delayMinutes,
        String alertLevel
) {

    /**
     * Calcula los tiempos transcurridos, tiempo límite y estado de alerta para un detalle de comanda.
     * Un platillo se considera con tiempo excedido si se encuentra en estado RECIBIDO o EN_PREPARACION
     * y el tiempo transcurrido desde su recepción/inicio supera el tiempo estimado de preparación.
     *
     * @param detail Detalle de comanda a evaluar
     * @param now Instante de referencia actual
     * @return Objeto con los cálculos de retraso y estado de alerta
     */
    public static DishPreparationDelayInfo calculate(ComandaDetail detail, Instant now) {
        if (detail == null) {
            return new DishPreparationDelayInfo((short) 15, null, null, 0L, false, 0L, "NORMAL");
        }

        short estimatedTime = detail.getEstimatedTimeMinutes() > 0
                ? detail.getEstimatedTimeMinutes()
                : (detail.getDish() != null && detail.getDish().getPreparationTimeMinutes() != null && detail.getDish().getPreparationTimeMinutes() > 0
                    ? detail.getDish().getPreparationTimeMinutes()
                    : 15);

        Comanda comanda = detail.getComanda();
        Instant startTime = detail.getReceivedAt() != null
                ? detail.getReceivedAt()
                : (detail.getPreparationStartedAt() != null
                    ? detail.getPreparationStartedAt()
                    : (comanda != null && comanda.getSentAt() != null ? comanda.getSentAt() : (comanda != null ? comanda.getCreatedAt() : null)));

        Instant current = now != null ? now : Instant.now();
        long elapsedMinutes = startTime != null ? Math.max(0, Duration.between(startTime, current).toMinutes()) : 0L;
        Instant deadline = startTime != null ? startTime.plus(Duration.ofMinutes(estimatedTime)) : null;

        boolean isPending = detail.getStatus() == ComandaDetailStatus.RECIBIDO || detail.getStatus() == ComandaDetailStatus.EN_PREPARACION;
        boolean timeExceeded = isPending && deadline != null && (current.isAfter(deadline) || elapsedMinutes > estimatedTime);
        long delayMinutes = timeExceeded ? Math.max(1L, elapsedMinutes - estimatedTime) : 0L;
        String alertLevel = timeExceeded ? "TIEMPO_EXCEDIDO" : "NORMAL";

        return new DishPreparationDelayInfo(
                estimatedTime,
                startTime,
                deadline,
                elapsedMinutes,
                timeExceeded,
                delayMinutes,
                alertLevel
        );
    }
}
