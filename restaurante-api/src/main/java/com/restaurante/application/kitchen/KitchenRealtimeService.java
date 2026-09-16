package com.restaurante.application.kitchen;

import com.restaurante.web.dto.kitchen.DishPreparationStatusResponse;
import com.restaurante.web.dto.kitchen.DishUnavailableResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio para la gestión de suscripciones SSE y notificación en tiempo real de comandas para cocina.
 */
@Service
public class KitchenRealtimeService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;
    private static final long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30 minutos

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emittersByRestaurant = new ConcurrentHashMap<>();

    /**
     * Registra un nuevo suscriptor SSE para la pantalla de cocina del restaurante.
     *
     * @param restaurantId Identificador del restaurante
     * @return Instancia de SseEmitter lista para recibir eventos
     */
    public SseEmitter subscribe(Long restaurantId) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.computeIfAbsent(
                targetRestaurantId, k -> new CopyOnWriteArrayList<>()
        );
        list.add(emitter);

        Runnable cleanup = () -> {
            CopyOnWriteArrayList<SseEmitter> ems = emittersByRestaurant.get(targetRestaurantId);
            if (ems != null) {
                ems.remove(emitter);
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(ex -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("conectado")
                    .data("Conexión en tiempo real con cocina establecida exitosamente"));
        } catch (IOException e) {
            cleanup.run();
        }

        return emitter;
    }

    /**
     * Notifica en tiempo real una nueva comanda entrante a todos los suscriptores activos de cocina.
     *
     * @param restaurantId Identificador del restaurante
     * @param comanda Datos de la comanda entrante
     */
    public void notifyNewComanda(Long restaurantId, KitchenComandaResponse comanda) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("nueva-comanda")
                        .data(comanda));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }

    /**
     * Notifica en tiempo real el cambio de estado de preparación de un platillo a todos los suscriptores activos.
     *
     * @param restaurantId Identificador del restaurante
     * @param dishStatus Datos del platillo con su nuevo estado
     */
    public void notifyDishStatusChanged(Long restaurantId, DishPreparationStatusResponse dishStatus) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("estado-platillo-actualizado")
                        .data(dishStatus));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }

    /**
     * Notifica en tiempo real una comanda actualizada a todos los suscriptores activos.
     *
     * @param restaurantId Identificador del restaurante
     * @param comanda Datos actualizados de la comanda
     */
    public void notifyComandaUpdated(Long restaurantId, KitchenComandaResponse comanda) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("comanda-actualizada")
                        .data(comanda));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }

    /**
     * Notifica en tiempo real que un platillo fue marcado como no disponible por falta de insumos a todos los suscriptores activos.
     *
     * @param restaurantId Identificador del restaurante
     * @param response Datos del platillo no disponible y su estado
     */
    public void notifyDishUnavailable(Long restaurantId, DishUnavailableResponse response) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("platillo-no-disponible")
                        .data(response));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }

    /**
     * Notifica en tiempo real que un platillo fue servido y entregado en la mesa a todos los suscriptores activos.
     *
     * @param restaurantId Identificador del restaurante
     * @param response Datos del platillo entregado
     */
    public void notifyDishDelivered(Long restaurantId, Object response) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("platillo-entregado")
                        .data(response));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }

    /**
     * Notifica en tiempo real una alerta por tiempo de preparación excedido a todos los suscriptores activos.
     *
     * @param restaurantId Identificador del restaurante
     * @param alertData Datos del platillo o comanda en estado de alerta
     */
    public void notifyPreparationTimeExceeded(Long restaurantId, Object alertData) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        CopyOnWriteArrayList<SseEmitter> list = emittersByRestaurant.get(targetRestaurantId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alerta-tiempo-excedido")
                        .data(alertData));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }
}
