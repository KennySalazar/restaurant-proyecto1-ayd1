package com.restaurante.application.kitchen;

import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.ComandaDetailModifier;
import com.restaurante.domain.model.ComandaDetailStatus;
import com.restaurante.domain.model.ComandaStatus;
import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.kitchen.DishPreparationStatusResponse;
import com.restaurante.web.dto.kitchen.DishUnavailableResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaItemResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import com.restaurante.web.dto.kitchen.MarkDishUnavailableRequest;
import com.restaurante.web.dto.kitchen.UpdateDishPreparationStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión operativa y visualización de comandas en cocina.
 */
@Service
public class KitchenService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ComandaRepository comandaRepository;
    private final ComandaDetailRepository comandaDetailRepository;
    private final RestaurantTableRepository tableRepository;
    private final RestaurantUserProfileRepository userProfileRepository;
    private final KitchenRealtimeService kitchenRealtimeService;
    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;
    private final DishRepository dishRepository;

    public KitchenService(ComandaRepository comandaRepository,
                          ComandaDetailRepository comandaDetailRepository,
                          RestaurantTableRepository tableRepository,
                          RestaurantUserProfileRepository userProfileRepository,
                          KitchenRealtimeService kitchenRealtimeService,
                          NotificationRepository notificationRepository,
                          RoleRepository roleRepository,
                          DishRepository dishRepository) {
        this.comandaRepository = comandaRepository;
        this.comandaDetailRepository = comandaDetailRepository;
        this.tableRepository = tableRepository;
        this.userProfileRepository = userProfileRepository;
        this.kitchenRealtimeService = kitchenRealtimeService;
        this.notificationRepository = notificationRepository;
        this.roleRepository = roleRepository;
        this.dishRepository = dishRepository;
    }

    /**
     * Consulta las comandas activas en cocina ordenadas de la más antigua a la más reciente.
     *
     * @param restaurantId Identificador del restaurante
     * @param statusFilter Filtro opcional por estado específico (por defecto RECIBIDA y EN_PREPARACION)
     * @return Listado de comandas ordenadas por antigüedad con sus platillos y tiempos estimados
     */
    @Transactional(readOnly = true)
    public List<KitchenComandaResponse> getActiveKitchenComandas(Long restaurantId, ComandaStatus statusFilter) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;

        List<ComandaStatus> statuses = (statusFilter != null)
                ? List.of(statusFilter)
                : List.of(ComandaStatus.RECIBIDA, ComandaStatus.EN_PREPARACION);

        List<Comanda> comandas = comandaRepository.findKitchenComandasByStatusIn(targetRestaurantId, statuses);

        if (comandas.isEmpty()) {
            return List.of();
        }

        List<Long> comandaIds = comandas.stream().map(Comanda::getId).toList();
        comandaDetailRepository.findByComandaIdInWithModifiers(comandaIds);

        List<Long> tableIds = comandas.stream()
                .map(c -> c.getAccount() != null ? c.getAccount().getTableId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, RestaurantTable> tableMap = tableIds.isEmpty()
                ? Map.of()
                : tableRepository.findAllById(tableIds).stream()
                .collect(Collectors.toMap(RestaurantTable::getId, t -> t));

        return comandas.stream()
                .map(c -> mapToKitchenResponse(c, tableMap))
                .toList();
    }

    /**
     * Consulta el detalle completo de una comanda específica para cocina.
     *
     * @param comandaId Identificador de la comanda
     * @param restaurantId Identificador del restaurante
     * @return Datos consolidados de la comanda para cocina
     */
    @Transactional(readOnly = true)
    public KitchenComandaResponse getKitchenComandaById(Long comandaId, Long restaurantId) {
        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;

        Comanda comanda = comandaRepository.findByIdWithDetailsAndRestaurantId(comandaId, targetRestaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "comanda_not_found",
                        "Comanda no encontrada",
                        "No se encontró una comanda con el identificador " + comandaId
                ));

        comandaDetailRepository.findByComandaIdWithModifiers(comandaId);

        RestaurantTable table = null;
        if (comanda.getAccount() != null && comanda.getAccount().getTableId() != null) {
            table = tableRepository.findById(comanda.getAccount().getTableId()).orElse(null);
        }

        return mapToKitchenResponse(comanda, table != null ? Map.of(table.getId(), table) : Map.of());
    }

    /**
     * Suscribe un cliente al flujo de eventos en tiempo real de la cocina.
     *
     * @param restaurantId Identificador del restaurante
     * @return Instancia de SseEmitter
     */
    public SseEmitter subscribeToKitchenStream(Long restaurantId) {
        return kitchenRealtimeService.subscribe(restaurantId);
    }

    /**
     * Convierte una entidad Comanda al DTO de visualización en cocina.
     *
     * @param comanda Entidad comanda
     * @return DTO consolidado para cocina
     */
    @Transactional(readOnly = true)
    public KitchenComandaResponse mapToKitchenResponse(Comanda comanda) {
        RestaurantTable table = null;
        if (comanda.getAccount() != null && comanda.getAccount().getTableId() != null) {
            table = tableRepository.findById(comanda.getAccount().getTableId()).orElse(null);
        }
        return mapToKitchenResponse(comanda, table != null ? Map.of(table.getId(), table) : Map.of());
    }

    private KitchenComandaResponse mapToKitchenResponse(Comanda comanda, Map<Long, RestaurantTable> tableMap) {
        Long tableId = comanda.getAccount() != null ? comanda.getAccount().getTableId() : null;
        RestaurantTable table = tableId != null ? tableMap.get(tableId) : null;
        String tableNumber = table != null ? table.getNumber() : "";

        String waiterName = null;
        if (comanda.getWaiterUser() != null) {
            waiterName = (comanda.getWaiterUser().getFirstName() + " " + comanda.getWaiterUser().getLastName()).trim();
        } else if (comanda.getWaiterId() != null) {
            waiterName = userProfileRepository.findById(comanda.getWaiterId())
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse("Mesero #" + comanda.getWaiterId());
        }

        Instant referenceTime = comanda.getSentAt() != null ? comanda.getSentAt() : comanda.getCreatedAt();
        long elapsedMinutes = referenceTime != null
                ? Math.max(0, Duration.between(referenceTime, Instant.now()).toMinutes())
                : 0;

        List<KitchenComandaItemResponse> itemResponses = new ArrayList<>();
        short maxPrepTime = 0;

        if (comanda.getDetails() != null) {
            for (ComandaDetail detail : comanda.getDetails()) {
                // Solo mostrar platillos que son relevantes para preparación en cocina
                if (detail.getStatus() == ComandaDetailStatus.BORRADOR
                        || detail.getStatus() == ComandaDetailStatus.NO_DISPONIBLE) {
                    continue;
                }

                short itemPrepTime = detail.getEstimatedTimeMinutes() > 0
                        ? detail.getEstimatedTimeMinutes()
                        : (short) 15;

                if (itemPrepTime > maxPrepTime) {
                    maxPrepTime = itemPrepTime;
                }

                List<String> modNames = new ArrayList<>();
                if (detail.getModifiers() != null) {
                    for (ComandaDetailModifier mod : detail.getModifiers()) {
                        modNames.add(mod.getNameSnapshot());
                    }
                }

                itemResponses.add(new KitchenComandaItemResponse(
                        detail.getId(),
                        detail.getDish() != null ? detail.getDish().getId() : null,
                        detail.getCombo() != null ? detail.getCombo().getId() : null,
                        detail.getNameSnapshot(),
                        detail.getQuantity(),
                        detail.getStatus().name(),
                        itemPrepTime,
                        detail.getSpecialNotes(),
                        modNames
                ));
            }
        }

        short estimatedPreparationTimeMinutes = maxPrepTime > 0 ? maxPrepTime : (short) 15;

        return new KitchenComandaResponse(
                comanda.getId(),
                comanda.getAccount() != null ? comanda.getAccount().getId() : null,
                comanda.getAccount() != null ? comanda.getAccount().getAccountNumber() : null,
                tableId,
                tableNumber,
                comanda.getRoundNumber(),
                comanda.getWaiterId(),
                waiterName,
                comanda.getStatus().name(),
                comanda.getSentAt(),
                comanda.getCreatedAt(),
                elapsedMinutes,
                estimatedPreparationTimeMinutes,
                comanda.getGeneralNotes(),
                itemResponses
        );
    }

    /**
     * Actualiza el estado de preparación de un platillo de comanda en cocina.
     * Valida las transiciones permitidas (recibido -> en preparación -> listo) e impide transiciones inválidas.
     * Notifica en tiempo real y alerta al mesero responsable cuando el platillo queda listo para servir.
     *
     * @param detailId Identificador del detalle de la comanda
     * @param request Solicitud con el nuevo estado
     * @param authentication Datos de autenticación del usuario operador
     * @return Respuesta con los datos actualizados del platillo y la comanda
     */
    @Transactional
    public DishPreparationStatusResponse updateDishPreparationStatus(
            Long detailId,
            UpdateDishPreparationStatusRequest request,
            Authentication authentication) {

        if (detailId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "detail_id_required",
                    "Identificador requerido",
                    "El identificador del platillo de la comanda es obligatorio"
            );
        }

        if (request == null || request.status() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "target_status_required",
                    "Estado requerido",
                    "El nuevo estado de preparación del platillo es obligatorio"
            );
        }

        ComandaDetail detail = comandaDetailRepository.findByIdWithComandaAndModifiers(detailId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_detail_not_found",
                        "Platillo no encontrado",
                        "No se encontró el detalle de comanda con identificador " + detailId
                ));

        Comanda comanda = detail.getComanda();
        if (comanda == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "comanda_not_found",
                    "Comanda no encontrada",
                    "El platillo no está asociado a una comanda válida"
            );
        }

        Long restaurantId = (comanda.getAccount() != null && comanda.getAccount().getRestaurantId() != null)
                ? comanda.getAccount().getRestaurantId()
                : DEFAULT_RESTAURANT_ID;

        ComandaDetailStatus currentStatus = detail.getStatus();
        ComandaDetailStatus targetStatus = request.status();

        // Si ya se encuentra en el estado solicitado, responder de forma idempotente
        if (currentStatus == targetStatus) {
            String tableNumber = resolveTableNumber(comanda);
            return buildDishStatusResponse(
                    detail,
                    comanda,
                    tableNumber,
                    currentStatus,
                    targetStatus,
                    "El platillo ya se encuentra en estado " + targetStatus.name()
            );
        }

        // Si se solicita marcar como no disponible, delegar a la lógica especializada
        if (targetStatus == ComandaDetailStatus.NO_DISPONIBLE) {
            DishUnavailableResponse unavailableResp = markDishAsUnavailable(
                    detailId,
                    new MarkDishUnavailableRequest("Marcado no disponible desde actualización de estado", true),
                    authentication
            );
            return new DishPreparationStatusResponse(
                    unavailableResp.id(),
                    unavailableResp.comandaId(),
                    unavailableResp.accountId(),
                    unavailableResp.tableId(),
                    unavailableResp.tableNumber(),
                    unavailableResp.dishName(),
                    unavailableResp.quantity(),
                    unavailableResp.previousStatus(),
                    unavailableResp.currentStatus(),
                    detail.getPreparationStartedAt(),
                    detail.getReadyAt(),
                    unavailableResp.comandaStatus(),
                    unavailableResp.message()
            );
        }

        // Validar transiciones permitidas según el ciclo de vida en cocina
        switch (currentStatus) {
            case BORRADOR -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_in_draft_state",
                    "Platillo en borrador",
                    "No se puede modificar la preparación de un platillo que aún está en borrador y no ha sido enviado a cocina"
            );
            case RECIBIDO -> {
                if (targetStatus == ComandaDetailStatus.LISTO) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_dish_status_transition",
                            "Transición de estado inválida",
                            "No se puede marcar el platillo directamente como LISTO saltando el estado EN_PREPARACION. El flujo requerido es RECIBIDO -> EN_PREPARACION -> LISTO."
                    );
                }
                if (targetStatus != ComandaDetailStatus.EN_PREPARACION) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_dish_status_transition",
                            "Transición de estado inválida",
                            "Desde el estado RECIBIDO solo se permite avanzar a EN_PREPARACION."
                    );
                }
            }
            case EN_PREPARACION -> {
                if (targetStatus != ComandaDetailStatus.LISTO) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_dish_status_transition",
                            "Transición de estado inválida",
                            "Desde el estado EN_PREPARACION solo se permite avanzar a LISTO."
                    );
                }
            }
            case LISTO -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_ready",
                    "Platillo ya listo",
                    "El platillo ya se encuentra en estado LISTO y no puede modificarse desde cocina. Debe ser servido por el mesero."
            );
            case ENTREGADO -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_delivered",
                    "Platillo ya entregado",
                    "El platillo ya fue entregado a la mesa y no puede modificarse desde cocina."
            );
            case NO_DISPONIBLE, CANCELADO -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_not_active",
                    "Platillo inactivo",
                    "No se puede cambiar el estado de un platillo cancelado o no disponible."
            );
        }

        Long responsibleUserId = resolveKitchenUserId(restaurantId, authentication);
        Instant now = Instant.now();

        detail.setStatus(targetStatus);
        detail.setUpdatedById(responsibleUserId);

        String tableNumber = resolveTableNumber(comanda);
        String message;

        if (targetStatus == ComandaDetailStatus.EN_PREPARACION) {
            if (detail.getPreparationStartedAt() == null) {
                detail.setPreparationStartedAt(now);
            }
            // Sincronizar comanda: si estaba RECIBIDA, pasa a EN_PREPARACION
            if (comanda.getStatus() == ComandaStatus.RECIBIDA) {
                comanda.setStatus(ComandaStatus.EN_PREPARACION);
                comandaRepository.save(comanda);
            }
            message = "Platillo '" + detail.getNameSnapshot() + "' marcado en preparación exitosamente";
        } else { // targetStatus == ComandaDetailStatus.LISTO
            if (detail.getReadyAt() == null) {
                detail.setReadyAt(now);
            }
            // Asegurar que la comanda esté al menos en EN_PREPARACION antes de evaluar LISTA
            if (comanda.getStatus() == ComandaStatus.RECIBIDA) {
                comanda.setStatus(ComandaStatus.EN_PREPARACION);
                comandaRepository.save(comanda);
            }

            // Notificar al mesero responsable que el platillo está listo para recoger y servir
            Role waiterRole = roleRepository.findByName(RoleName.WAITER).orElse(null);
            Long waiterRoleId = waiterRole != null ? waiterRole.getId() : null;

            String mesaInfo = (tableNumber != null && !tableNumber.isBlank()) ? "Mesa " + tableNumber : "la cuenta";
            Notification waiterNotification = new Notification(
                    restaurantId,
                    comanda.getWaiterId(),
                    waiterRoleId,
                    "PLATILLO_LISTO",
                    "Platillo listo para servir - " + mesaInfo,
                    "El platillo '" + detail.getNameSnapshot() + "' de la comanda #" + comanda.getId() + " (" + mesaInfo + ") está listo para ser recogido y servido.",
                    "COMANDA_DETALLE",
                    detail.getId().toString(),
                    "ALTA"
            );
            notificationRepository.save(waiterNotification);

            // Verificar si todos los platillos activos de la comanda están listos o entregados
            List<ComandaDetail> allDetails = comandaDetailRepository.findByComandaIdWithModifiers(comanda.getId());
            boolean allReadyOrDelivered = allDetails.stream()
                    .filter(d -> d.getStatus() != ComandaDetailStatus.CANCELADO && d.getStatus() != ComandaDetailStatus.NO_DISPONIBLE)
                    .allMatch(d -> d.getId().equals(detailId) || d.getStatus() == ComandaDetailStatus.LISTO || d.getStatus() == ComandaDetailStatus.ENTREGADO);

            if (allReadyOrDelivered) {
                comanda.setStatus(ComandaStatus.LISTA);
                comandaRepository.save(comanda);

                Notification comandaNotification = new Notification(
                        restaurantId,
                        comanda.getWaiterId(),
                        waiterRoleId,
                        "COMANDA_LISTA",
                        "Comanda completa lista para servir - " + mesaInfo,
                        "Todos los platillos de la comanda #" + comanda.getId() + " (" + mesaInfo + ") están listos para ser servidos.",
                        "COMANDA",
                        comanda.getId().toString(),
                        "ALTA"
                );
                notificationRepository.save(comandaNotification);
            }

            message = "Platillo '" + detail.getNameSnapshot() + "' marcado como listo. Se ha notificado al mesero para servirlo.";
        }

        detail = comandaDetailRepository.save(detail);

        DishPreparationStatusResponse response = buildDishStatusResponse(
                detail,
                comanda,
                tableNumber,
                currentStatus,
                targetStatus,
                message
        );

        // Notificar en tiempo real mediante SSE a cocina y meseros
        try {
            kitchenRealtimeService.notifyDishStatusChanged(restaurantId, response);
            KitchenComandaResponse kitchenPayload = mapToKitchenResponse(comanda);
            kitchenRealtimeService.notifyComandaUpdated(restaurantId, kitchenPayload);
        } catch (Exception ignored) {
        }

        return response;
    }

    private Long resolveKitchenUserId(Long restaurantId, Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            Long userId = jwtData.userId();
            if (jwtData.role() == RoleName.KITCHEN) {
                return userId;
            }
        }

        List<Long> kitchenUserIds = userProfileRepository.findActiveKitchenUsersByRestaurantId(restaurantId);
        if (!kitchenUserIds.isEmpty()) {
            return kitchenUserIds.get(0);
        }

        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.userId();
        }

        return 1L;
    }

    private String resolveTableNumber(Comanda comanda) {
        if (comanda.getAccount() != null && comanda.getAccount().getTableId() != null) {
            return tableRepository.findById(comanda.getAccount().getTableId())
                    .map(RestaurantTable::getNumber)
                    .orElse("");
        }
        return "";
    }

    private DishPreparationStatusResponse buildDishStatusResponse(
            ComandaDetail detail,
            Comanda comanda,
            String tableNumber,
            ComandaDetailStatus previousStatus,
            ComandaDetailStatus currentStatus,
            String message) {

        Long tableId = (comanda.getAccount() != null) ? comanda.getAccount().getTableId() : null;
        Long accountId = (comanda.getAccount() != null) ? comanda.getAccount().getId() : null;

        return new DishPreparationStatusResponse(
                detail.getId(),
                comanda.getId(),
                accountId,
                tableId,
                tableNumber,
                detail.getNameSnapshot(),
                detail.getQuantity(),
                previousStatus.name(),
                currentStatus.name(),
                detail.getPreparationStartedAt(),
                detail.getReadyAt(),
                comanda.getStatus().name(),
                message
        );
    }

    /**
     * Marca un platillo de comanda en cocina como no disponible por discrepancia o falta de insumos.
     * Notifica de inmediato al mesero responsable para informar al cliente y actualiza opcionalmente
     * la disponibilidad del platillo en el catálogo del menú para prevenir nuevos pedidos.
     *
     * @param detailId Identificador del detalle de la comanda
     * @param request Datos de la solicitud con motivo y opción de desactivar en menú
     * @param authentication Datos de autenticación del usuario operador de cocina
     * @return Respuesta con los datos del platillo marcado como no disponible y las acciones ejecutadas
     */
    @Transactional
    public DishUnavailableResponse markDishAsUnavailable(
            Long detailId,
            MarkDishUnavailableRequest request,
            Authentication authentication) {

        if (detailId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "detail_id_required",
                    "Identificador requerido",
                    "El identificador del platillo de la comanda es obligatorio"
            );
        }

        ComandaDetail detail = comandaDetailRepository.findByIdWithComandaAndModifiers(detailId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_detail_not_found",
                        "Platillo no encontrado",
                        "No se encontró el detalle de comanda con identificador " + detailId
                ));

        Comanda comanda = detail.getComanda();
        if (comanda == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "comanda_not_found",
                    "Comanda no encontrada",
                    "El platillo no está asociado a una comanda válida"
            );
        }

        ComandaDetailStatus currentStatus = detail.getStatus();

        // Validaciones de ciclo de vida
        if (currentStatus == ComandaDetailStatus.NO_DISPONIBLE) {
            String tableNumber = resolveTableNumber(comanda);
            String waiterName = resolveWaiterName(comanda);
            Long dishId = detail.getDish() != null ? detail.getDish().getId() : null;
            return new DishUnavailableResponse(
                    detail.getId(),
                    comanda.getId(),
                    comanda.getAccount() != null ? comanda.getAccount().getId() : null,
                    comanda.getAccount() != null ? comanda.getAccount().getTableId() : null,
                    tableNumber,
                    dishId,
                    detail.getNameSnapshot(),
                    detail.getQuantity(),
                    currentStatus.name(),
                    ComandaDetailStatus.NO_DISPONIBLE.name(),
                    request != null ? request.reason() : null,
                    detail.getDish() != null && !detail.getDish().isManualAvailable(),
                    comanda.getWaiterId(),
                    waiterName,
                    comanda.getStatus().name(),
                    Instant.now(),
                    "El platillo ya se encontraba marcado como no disponible"
            );
        }

        if (currentStatus == ComandaDetailStatus.BORRADOR) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_in_draft_state",
                    "Platillo en borrador",
                    "No se puede marcar como no disponible un platillo que aún está en borrador y no ha sido enviado a cocina"
            );
        }

        if (currentStatus == ComandaDetailStatus.LISTO) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_ready",
                    "Platillo ya listo",
                    "El platillo ya fue preparado y está listo para servir; no puede marcarse como no disponible por falta de insumo"
            );
        }

        if (currentStatus == ComandaDetailStatus.ENTREGADO) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_delivered",
                    "Platillo ya entregado",
                    "El platillo ya fue entregado a la mesa y no puede modificarse desde cocina"
            );
        }

        if (currentStatus == ComandaDetailStatus.CANCELADO) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_cancelled",
                    "Platillo cancelado",
                    "El platillo ya fue cancelado previamente"
            );
        }

        Long restaurantId = (comanda.getAccount() != null && comanda.getAccount().getRestaurantId() != null)
                ? comanda.getAccount().getRestaurantId()
                : DEFAULT_RESTAURANT_ID;

        Long responsibleUserId = resolveKitchenUserId(restaurantId, authentication);
        Instant now = Instant.now();

        detail.setStatus(ComandaDetailStatus.NO_DISPONIBLE);
        detail.setUpdatedById(responsibleUserId);

        // Si se solicita o por defecto, actualizar disponibilidad en el menú (App1)
        boolean menuDisabled = false;
        boolean shouldDisableInMenu = request == null || request.disableInMenu() == null || Boolean.TRUE.equals(request.disableInMenu());
        if (shouldDisableInMenu && detail.getDish() != null) {
            Dish dish = detail.getDish();
            dish.setManualAvailable(false);
            dishRepository.save(dish);
            menuDisabled = true;
        }

        String tableNumber = resolveTableNumber(comanda);
        String waiterName = resolveWaiterName(comanda);
        String mesaInfo = (tableNumber != null && !tableNumber.isBlank()) ? "Mesa " + tableNumber : "la cuenta";

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Discrepancia o falta de insumos detectada en cocina";

        // Notificar al mesero correspondiente para que informe al cliente
        Role waiterRole = roleRepository.findByName(RoleName.WAITER).orElse(null);
        Long waiterRoleId = waiterRole != null ? waiterRole.getId() : null;

        Notification waiterNotification = new Notification(
                restaurantId,
                comanda.getWaiterId(),
                waiterRoleId,
                "PLATILLO_NO_DISPONIBLE_COCINA",
                "Platillo no disponible - " + mesaInfo,
                "El platillo '" + detail.getNameSnapshot() + "' de la comanda #" + comanda.getId() +
                        " (" + mesaInfo + ") fue marcado como NO DISPONIBLE en cocina: " + reason +
                        ". Por favor informe al cliente para seleccionar otra opción o retirar el ítem.",
                "COMANDA_DETALLE",
                detail.getId().toString(),
                "ALTA"
        );
        notificationRepository.save(waiterNotification);

        // Notificar al administrador sobre la discrepancia de inventario detectada en cocina
        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElse(null);
        Long adminRoleId = adminRole != null ? adminRole.getId() : null;

        Notification adminNotification = new Notification(
                restaurantId,
                null,
                adminRoleId,
                "DISCREPANCIA_INVENTARIO_COCINA",
                "Discrepancia de inventario en cocina - " + detail.getNameSnapshot(),
                "Cocina reportó falta de insumos para preparar '" + detail.getNameSnapshot() +
                        "' en la comanda #" + comanda.getId() + " (" + mesaInfo + "): " + reason +
                        (menuDisabled ? ". El platillo fue desactivado temporalmente en el menú." : "") +
                        " Se sugiere verificar el inventario físico.",
                "COMANDA_DETALLE",
                detail.getId().toString(),
                "ALTA"
        );
        notificationRepository.save(adminNotification);

        // Sincronizar estado de la comanda si todos los platillos quedan listos o inactivos
        final Long currentDetailId = detail.getId();
        List<ComandaDetail> allDetails = comandaDetailRepository.findByComandaIdWithModifiers(comanda.getId());

        boolean allCancelledOrUnavailable = allDetails.stream()
                .allMatch(d -> d.getId().equals(currentDetailId) || d.getStatus() == ComandaDetailStatus.CANCELADO || d.getStatus() == ComandaDetailStatus.NO_DISPONIBLE);

        boolean allRemainingReadyOrDelivered = !allCancelledOrUnavailable && allDetails.stream()
                .filter(d -> !d.getId().equals(currentDetailId) && d.getStatus() != ComandaDetailStatus.CANCELADO && d.getStatus() != ComandaDetailStatus.NO_DISPONIBLE)
                .allMatch(d -> d.getStatus() == ComandaDetailStatus.LISTO || d.getStatus() == ComandaDetailStatus.ENTREGADO);

        if (allCancelledOrUnavailable) {
            if (comanda.getStatus() == ComandaStatus.RECIBIDA || comanda.getStatus() == ComandaStatus.EN_PREPARACION) {
                comanda.setStatus(ComandaStatus.CANCELADA);
                comandaRepository.save(comanda);
            }
        } else if (allRemainingReadyOrDelivered) {
            comanda.setStatus(ComandaStatus.LISTA);
            comandaRepository.save(comanda);
        }

        detail = comandaDetailRepository.save(detail);

        String message = "Platillo '" + detail.getNameSnapshot() + "' marcado como no disponible. Se ha notificado al mesero para informar al cliente" +
                (menuDisabled ? " y se deshabilitó en el menú." : ".");

        DishUnavailableResponse response = new DishUnavailableResponse(
                detail.getId(),
                comanda.getId(),
                comanda.getAccount() != null ? comanda.getAccount().getId() : null,
                comanda.getAccount() != null ? comanda.getAccount().getTableId() : null,
                tableNumber,
                detail.getDish() != null ? detail.getDish().getId() : null,
                detail.getNameSnapshot(),
                detail.getQuantity(),
                currentStatus.name(),
                ComandaDetailStatus.NO_DISPONIBLE.name(),
                reason,
                menuDisabled,
                comanda.getWaiterId(),
                waiterName,
                comanda.getStatus().name(),
                now,
                message
        );

        // Notificar en tiempo real por SSE a cocina y meseros
        try {
            kitchenRealtimeService.notifyDishUnavailable(restaurantId, response);
            KitchenComandaResponse kitchenPayload = mapToKitchenResponse(comanda);
            kitchenRealtimeService.notifyComandaUpdated(restaurantId, kitchenPayload);
        } catch (Exception ignored) {
        }

        return response;
    }

    private String resolveWaiterName(Comanda comanda) {
        if (comanda.getWaiterUser() != null) {
            return (comanda.getWaiterUser().getFirstName() + " " + comanda.getWaiterUser().getLastName()).trim();
        } else if (comanda.getWaiterId() != null) {
            return userProfileRepository.findById(comanda.getWaiterId())
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse("Mesero #" + comanda.getWaiterId());
        }
        return "Mesero no asignado";
    }
}
