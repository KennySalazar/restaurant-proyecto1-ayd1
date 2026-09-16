package com.restaurante.application.kitchen;

import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.ComandaDetailModifier;
import com.restaurante.domain.model.ComandaDetailStatus;
import com.restaurante.domain.model.ComandaStatus;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.kitchen.KitchenComandaItemResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import org.springframework.http.HttpStatus;
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

    public KitchenService(ComandaRepository comandaRepository,
                          ComandaDetailRepository comandaDetailRepository,
                          RestaurantTableRepository tableRepository,
                          RestaurantUserProfileRepository userProfileRepository,
                          KitchenRealtimeService kitchenRealtimeService) {
        this.comandaRepository = comandaRepository;
        this.comandaDetailRepository = comandaDetailRepository;
        this.tableRepository = tableRepository;
        this.userProfileRepository = userProfileRepository;
        this.kitchenRealtimeService = kitchenRealtimeService;
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
}
