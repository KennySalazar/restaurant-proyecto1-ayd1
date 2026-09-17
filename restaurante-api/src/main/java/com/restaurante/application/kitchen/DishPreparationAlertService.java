package com.restaurante.application.kitchen;

import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.DishPreparationDelayInfo;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio encargado de la evaluación proactiva y generación de alertas por tiempo
 * de preparación de platillos excedido en cocina y para los meseros responsables.
 */
@Service
public class DishPreparationAlertService {

    private static final Logger log = LoggerFactory.getLogger(DishPreparationAlertService.class);
    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ComandaDetailRepository comandaDetailRepository;
    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;
    private final RestaurantTableRepository tableRepository;
    private final KitchenRealtimeService kitchenRealtimeService;

    public DishPreparationAlertService(
            ComandaDetailRepository comandaDetailRepository,
            NotificationRepository notificationRepository,
            RoleRepository roleRepository,
            RestaurantTableRepository tableRepository,
            KitchenRealtimeService kitchenRealtimeService) {
        this.comandaDetailRepository = comandaDetailRepository;
        this.notificationRepository = notificationRepository;
        this.roleRepository = roleRepository;
        this.tableRepository = tableRepository;
        this.kitchenRealtimeService = kitchenRealtimeService;
    }

    /**
     * Tarea programada que evalúa periódicamente los platillos activos en cocina
     * y genera alertas visuales y notificaciones si superaron su tiempo estimado sin estar listos.
     */
    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void evaluateOverdueDishesPeriodically() {
        try {
            evaluateRestaurantDishDelays(DEFAULT_RESTAURANT_ID);
        } catch (Exception e) {
            log.debug("Error evaluando alertas de tiempo de preparación excedido: {}", e.getMessage());
        }
    }

    /**
     * Evalúa los platillos en preparación o recibidos en cocina para un restaurante dado,
     * emitiendo notificaciones a meseros y cocina cuando se detectan retrasos.
     *
     * @param restaurantId Identificador del restaurante
     * @return Lista de detalles de comanda con tiempo excedido
     */
    @Transactional
    public List<ComandaDetail> evaluateRestaurantDishDelays(Long restaurantId) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        List<ComandaDetail> activeDishes = comandaDetailRepository.findActiveDishesInKitchen(targetRestaurantId);

        Instant now = Instant.now();
        List<ComandaDetail> delayedDishes = new ArrayList<>();

        for (ComandaDetail detail : activeDishes) {
            DishPreparationDelayInfo delayInfo = DishPreparationDelayInfo.calculate(detail, now);
            if (delayInfo.timeExceeded()) {
                delayedDishes.add(detail);
                notifyDelayedDishIfNew(targetRestaurantId, detail, delayInfo);
            }
        }

        return delayedDishes;
    }

    private void notifyDelayedDishIfNew(Long restaurantId, ComandaDetail detail, DishPreparationDelayInfo delayInfo) {
        String entityId = detail.getId().toString();
        boolean alreadyNotified = notificationRepository.existsByRestaurantIdAndTypeAndEntityAndEntityIdAndReadFalse(
                restaurantId,
                "TIEMPO_PREPARACION_EXCEDIDO",
                "COMANDA_DETALLE",
                entityId
        );

        if (alreadyNotified) {
            return;
        }

        Comanda comanda = detail.getComanda();
        String mesaText = "Mesa";
        if (comanda != null && comanda.getAccount() != null && comanda.getAccount().getTableId() != null) {
            mesaText = tableRepository.findById(comanda.getAccount().getTableId())
                    .map(RestaurantTable::getNumber)
                    .map(num -> "Mesa " + num)
                    .orElse("Mesa #" + comanda.getAccount().getTableId());
        }

        Long waiterId = comanda != null ? comanda.getWaiterId() : null;
        Role waiterRole = roleRepository.findByName(RoleName.WAITER).orElse(null);
        Long waiterRoleId = waiterRole != null ? waiterRole.getId() : null;

        String title = "Tiempo de preparación excedido - " + mesaText;
        String message = String.format(
                "El platillo '%s' ha superado su tiempo estimado de preparación (%d min) y lleva %d min sin estar listo.",
                detail.getNameSnapshot(),
                delayInfo.estimatedTimeMinutes(),
                delayInfo.elapsedMinutes()
        );

        Notification notification = new Notification(
                restaurantId,
                waiterId,
                waiterRoleId,
                "TIEMPO_PREPARACION_EXCEDIDO",
                title,
                message,
                "COMANDA_DETALLE",
                entityId,
                "ALTA"
        );
        notificationRepository.save(notification);

        // Notificar por SSE a las pantallas activas de cocina y meseros
        try {
            kitchenRealtimeService.notifyPreparationTimeExceeded(restaurantId, notification);
        } catch (Exception ignored) {
        }
    }
}
