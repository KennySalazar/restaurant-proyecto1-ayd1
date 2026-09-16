package com.restaurante.application.inventory;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.ComandaDetailModifier;
import com.restaurante.domain.model.ComandaDetailStatus;
import com.restaurante.domain.model.ComandaStatus;
import com.restaurante.domain.model.Combo;
import com.restaurante.domain.model.ComboDetail;
import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.InventoryMovement;
import com.restaurante.domain.model.Modifier;
import com.restaurante.domain.model.ModifierRecipeDetail;
import com.restaurante.domain.model.ModifierRecipeVersion;
import com.restaurante.domain.model.RecipeDetail;
import com.restaurante.domain.model.RecipeVersion;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.ComboDetailRepository;
import com.restaurante.domain.repository.ComboRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.application.kitchen.KitchenRealtimeService;
import com.restaurante.application.kitchen.KitchenService;
import com.restaurante.domain.repository.InventoryMovementRepository;
import com.restaurante.domain.repository.ModifierRecipeDetailRepository;
import com.restaurante.domain.repository.ModifierRecipeVersionRepository;
import com.restaurante.domain.repository.ModifierRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.account.AccountRoundResponse;
import com.restaurante.web.dto.account.CreateAccountRoundRequest;
import com.restaurante.web.dto.comanda.AddDishItemRequest;
import com.restaurante.web.dto.comanda.AddDishResponse;
import com.restaurante.web.dto.comanda.AddDishToAccountRequest;
import com.restaurante.web.dto.comanda.ComandaDishProgressResponse;
import com.restaurante.web.dto.comanda.ComandaInventoryProcessResponse;
import com.restaurante.web.dto.kitchen.KitchenComandaResponse;
import com.restaurante.web.dto.comanda.ComandaItemResponse;
import com.restaurante.web.dto.comanda.ComandaResponse;
import com.restaurante.web.dto.comanda.CreateComandaItemRequest;
import com.restaurante.web.dto.comanda.CreateComandaRequest;
import com.restaurante.web.dto.comanda.DeliverDishRequest;
import com.restaurante.web.dto.comanda.DishOrderDetailResponse;
import com.restaurante.web.dto.comanda.KardexMovementResponse;
import com.restaurante.web.dto.comanda.RejectedDishDetailResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para el procesamiento de comandas y descuento automático de inventario.
 */
@Service
public class ComandaInventoryService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ComandaRepository comandaRepository;
    private final ComandaDetailRepository comandaDetailRepository;
    private final AccountRepository accountRepository;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;
    private final ComboDetailRepository comboDetailRepository;
    private final RecipeVersionRepository recipeVersionRepository;
    private final ModifierRepository modifierRepository;
    private final ModifierRecipeVersionRepository modifierRecipeVersionRepository;
    private final ModifierRecipeDetailRepository modifierRecipeDetailRepository;
    private final SupplyRepository supplyRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final RestaurantTableRepository tableRepository;
    private final RestaurantUserProfileRepository userProfileRepository;
    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;
    private final KitchenRealtimeService kitchenRealtimeService;
    private final KitchenService kitchenService;

    @PersistenceContext
    private EntityManager entityManager;

    public ComandaInventoryService(ComandaRepository comandaRepository,
                                   ComandaDetailRepository comandaDetailRepository,
                                   AccountRepository accountRepository,
                                   DishRepository dishRepository,
                                   ComboRepository comboRepository,
                                   ComboDetailRepository comboDetailRepository,
                                   RecipeVersionRepository recipeVersionRepository,
                                   ModifierRepository modifierRepository,
                                   ModifierRecipeVersionRepository modifierRecipeVersionRepository,
                                   ModifierRecipeDetailRepository modifierRecipeDetailRepository,
                                   SupplyRepository supplyRepository,
                                   InventoryMovementRepository inventoryMovementRepository,
                                   RestaurantTableRepository tableRepository,
                                   RestaurantUserProfileRepository userProfileRepository,
                                   NotificationRepository notificationRepository,
                                   RoleRepository roleRepository,
                                   KitchenRealtimeService kitchenRealtimeService,
                                   KitchenService kitchenService) {
        this.comandaRepository = comandaRepository;
        this.comandaDetailRepository = comandaDetailRepository;
        this.accountRepository = accountRepository;
        this.dishRepository = dishRepository;
        this.comboRepository = comboRepository;
        this.comboDetailRepository = comboDetailRepository;
        this.recipeVersionRepository = recipeVersionRepository;
        this.modifierRepository = modifierRepository;
        this.modifierRecipeVersionRepository = modifierRecipeVersionRepository;
        this.modifierRecipeDetailRepository = modifierRecipeDetailRepository;
        this.supplyRepository = supplyRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.tableRepository = tableRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationRepository = notificationRepository;
        this.roleRepository = roleRepository;
        this.kitchenRealtimeService = kitchenRealtimeService;
        this.kitchenService = kitchenService;
    }

    /**
     * Registra una comanda en estado inicial BORRADOR asociada a una mesa o cuenta.
     *
     * @param request Datos de la orden, platillos y modificadores
     * @param authentication Información de autenticación del usuario mesero o administrador
     * @return Confirmación y datos de la comanda registrada
     */
    @Transactional
    public ComandaInventoryProcessResponse createComanda(CreateComandaRequest request, Authentication authentication) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "comanda_without_items",
                    "Comanda vacía",
                    "La comanda debe contener al menos un platillo"
            );
        }

        Long waiterId = resolveWaiterUserId(restaurantId, authentication);
        Account account = resolveOrCreateAccount(restaurantId, request, waiterId);

        if ("LISTA_COBRO".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_already_in_billing",
                    "Cuenta en proceso de cobro",
                    "No se pueden registrar comandas para una cuenta marcada como lista para cobro"
            );
        }
        if (!"ABIERTA".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta inválido",
                    "Solo se pueden registrar comandas para cuentas en estado abierta"
            );
        }

        short nextRound = (short) (comandaRepository.findMaxRoundNumberByAccountId(account.getId()) + 1);
        Comanda comanda = new Comanda(
                account,
                nextRound,
                waiterId,
                request.generalNotes() != null ? request.generalNotes().trim() : null
        );
        Comanda savedComanda = comandaRepository.save(comanda);

        processItemsForComanda(restaurantId, savedComanda, request.items());

        validateSupplyAvailability(restaurantId, savedComanda);

        if (Boolean.TRUE.equals(request.sendImmediately())) {
            return sendComanda(savedComanda.getId(), authentication);
        }

        return new ComandaInventoryProcessResponse(
                "Comanda registrada exitosamente en borrador",
                mapToComandaResponse(savedComanda),
                List.of()
        );
    }

    /**
     * Envía una comanda a cocina y descuenta automáticamente los insumos del inventario en el kardex.
     * Valida individualmente el stock de cada platillo y modificador: si un platillo no tiene stock
     * suficiente de insumos, se rechaza su envío, se marca como no disponible y se notifica al mesero.
     * Los platillos con stock disponible se envían a cocina y se notifica al equipo de cocina en tiempo real.
     *
     * @param comandaId Identificador de la comanda a enviar
     * @param authentication Información de autenticación del usuario que procesa el envío
     * @return Confirmación del procesamiento, estado actualizado y movimientos de kardex generados
     */
    @Transactional(noRollbackFor = ApiException.class)
    public ComandaInventoryProcessResponse sendComanda(Long comandaId, Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);

        if (comandaId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_comanda_id",
                    "Comanda no especificada",
                    "Debe indicar el identificador de la comanda a procesar"
            );
        }

        Comanda comanda = comandaRepository
                .findByIdWithDetailsAndRestaurantId(comandaId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "comanda_not_found",
                        "Comanda no encontrada",
                        "No se encontró una comanda con el identificador " + comandaId
                ));
        comandaDetailRepository.findByComandaIdWithModifiers(comandaId);

        if (comanda.getStatus() != ComandaStatus.BORRADOR
                || inventoryMovementRepository.existsByComandaIdAndType(comandaId, "SALIDA_VENTA")) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "comanda_inventory_already_processed",
                    "Inventario ya procesado",
                    "El inventario de la comanda ya fue procesado"
            );
        }

        if (comanda.getDetails() == null || comanda.getDetails().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "comanda_without_items",
                    "Comanda vacía",
                    "No se puede enviar una comanda sin platillos"
            );
        }

        List<ComandaDetail> draftDetails = comanda.getDetails().stream()
                .filter(d -> d.getStatus() == ComandaDetailStatus.BORRADOR)
                .toList();

        if (draftDetails.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "no_draft_items",
                    "Sin platillos pendientes",
                    "La comanda no tiene platillos en borrador pendientes de enviar a cocina"
            );
        }

        Role waiterRole = roleRepository.findByName(RoleName.WAITER).orElse(null);
        Long waiterRoleId = waiterRole != null ? waiterRole.getId() : null;

        List<ComandaDetail> validDetails = new ArrayList<>();
        List<RejectedDishDetailResponse> rejectedDishes = new ArrayList<>();
        Map<Long, BigDecimal> availableStockMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalsToDeduct = new LinkedHashMap<>();
        Map<Long, ComandaDetail> detailBySupply = new LinkedHashMap<>();

        for (ComandaDetail detail : draftDetails) {
            Map<Long, BigDecimal> detailRequirements = calculateDetailSupplyRequirements(restaurantId, detail);
            boolean canFulfill = true;
            String rejectReason = null;

            for (Map.Entry<Long, BigDecimal> entry : detailRequirements.entrySet()) {
                Long supplyId = entry.getKey();
                BigDecimal requiredAmount = entry.getValue();

                if (requiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                    Supply supply = supplyRepository.findByIdAndRestaurantId(supplyId, restaurantId)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.NOT_FOUND,
                                    "supply_not_found",
                                    "Insumo no encontrado",
                                    "No se encontró el insumo con identificador " + supplyId
                            ));

                    if (!supply.isActive()) {
                        canFulfill = false;
                        rejectReason = "El platillo requiere el insumo inactivo '" + supply.getName() + "'";
                        break;
                    }

                    BigDecimal currentAvailable = availableStockMap.computeIfAbsent(supplyId, id -> supply.getCurrentStock());
                    if (currentAvailable.compareTo(requiredAmount) < 0) {
                        canFulfill = false;
                        rejectReason = "Stock insuficiente del insumo '" + supply.getName() + "' (requerido: " + requiredAmount + ", disponible: " + currentAvailable + ")";
                        break;
                    }
                }
            }

            if (!canFulfill) {
                // Rechazar el envío de este platillo específico
                detail.setStatus(ComandaDetailStatus.NO_DISPONIBLE);
                comandaDetailRepository.save(detail);

                // Notificar al mesero cuál platillo no pudo enviarse y por qué
                Notification waiterNotification = new Notification(
                        restaurantId,
                        comanda.getWaiterId(),
                        waiterRoleId,
                        "PLATILLO_RECHAZADO_STOCK",
                        "Platillo rechazado por falta de stock - " + detail.getNameSnapshot(),
                        "El platillo '" + detail.getNameSnapshot() + "' no pudo enviarse a cocina debido a stock insuficiente: " + rejectReason + ".",
                        "COMANDA_DETALLE",
                        detail.getId().toString(),
                        "ALTA"
                );
                notificationRepository.save(waiterNotification);

                rejectedDishes.add(new RejectedDishDetailResponse(
                        detail.getId(),
                        detail.getNameSnapshot(),
                        rejectReason
                ));
            } else {
                // Reservar el stock en memoria para no sobreasignar entre detalles de la misma comanda
                for (Map.Entry<Long, BigDecimal> entry : detailRequirements.entrySet()) {
                    Long supplyId = entry.getKey();
                    BigDecimal requiredAmount = entry.getValue();
                    if (requiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                        availableStockMap.put(supplyId, availableStockMap.get(supplyId).subtract(requiredAmount));
                        totalsToDeduct.merge(supplyId, requiredAmount, BigDecimal::add);
                        detailBySupply.putIfAbsent(supplyId, detail);
                    }
                }
                validDetails.add(detail);
            }
        }

        if (validDetails.isEmpty()) {
            // Todos los platillos fueron rechazados por stock insuficiente
            String errorSummary = rejectedDishes.stream()
                    .map(r -> r.dishName() + ": " + r.reason())
                    .collect(Collectors.joining("; "));
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_supply_stock",
                    "Stock insuficiente al enviar comanda",
                    "No fue posible enviar la comanda a cocina. Se rechazaron los platillos por falta de inventario: " + errorSummary
            );
        }

        // Transición de estado de los platillos válidos a RECIBIDO
        Instant now = Instant.now();
        for (ComandaDetail validDetail : validDetails) {
            validDetail.setStatus(ComandaDetailStatus.RECIBIDO);
            validDetail.setReceivedAt(now);
            comandaDetailRepository.save(validDetail);
        }

        // Transición de la comanda a RECIBIDA
        comanda.setStatus(ComandaStatus.RECIBIDA);
        comanda.setSentAt(now);

        try {
            comandaRepository.saveAndFlush(comanda);
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "inventory_update_failed",
                    "Error de inventario",
                    "No fue posible actualizar el inventario: " + ex.getMessage()
            );
        }

        // Obtener los movimientos registrados en el kardex (por trigger si activo)
        List<InventoryMovement> movements = inventoryMovementRepository.findByComandaIdWithSupply(comanda.getId());

        if (movements.isEmpty()) {
            // Mecanismo de respaldo para entornos donde los triggers de base de datos no aplican automáticamente
            for (Map.Entry<Long, BigDecimal> entry : totalsToDeduct.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    Supply supply = supplyRepository.findById(entry.getKey()).orElseThrow();
                    BigDecimal prevStock = supply.getCurrentStock();
                    BigDecimal newStock = prevStock.subtract(entry.getValue());
                    supply.setCurrentStock(newStock);
                    supplyRepository.save(supply);

                    ComandaDetail detailForMovement = detailBySupply.getOrDefault(entry.getKey(), validDetails.get(0));
                    InventoryMovement movement = new InventoryMovement(
                            restaurantId,
                            supply,
                            "SALIDA_VENTA",
                            entry.getValue(),
                            detailForMovement,
                            "Consumo automatico al enviar comanda " + comanda.getId(),
                            comanda.getWaiterId()
                    );
                    movement.setPreviousStock(prevStock);
                    movement.setResultingStock(newStock);
                    movement.setUnitCostSnapshot(supply.getCurrentUnitCost());
                    inventoryMovementRepository.save(movement);
                    movements.add(movement);
                }
            }
        } else {
            // Sincronizar entidades de insumos afectadas en el contexto de persistencia
            for (InventoryMovement movement : movements) {
                entityManager.refresh(movement.getSupply());
            }
        }

        // Notificar a cocina en tiempo real
        Role kitchenRole = roleRepository.findByName(RoleName.KITCHEN).orElse(null);
        Long kitchenRoleId = kitchenRole != null ? kitchenRole.getId() : null;

        RestaurantTable table = (comanda.getAccount() != null && comanda.getAccount().getTableId() != null)
                ? tableRepository.findById(comanda.getAccount().getTableId()).orElse(null)
                : null;
        String tableNumber = table != null ? table.getNumber() : "";

        Notification kitchenNotification = new Notification(
                restaurantId,
                null,
                kitchenRoleId,
                "COMANDA_ENVIADA_COCINA",
                "Nueva comanda recibida - Mesa " + tableNumber,
                "Comanda #" + comanda.getId() + " de la mesa " + tableNumber + " recibida en cocina con " + validDetails.size() + " platillo(s) para preparación.",
                "COMANDA",
                comanda.getId().toString(),
                "ALTA"
        );
        notificationRepository.save(kitchenNotification);

        // Emitir actualización en tiempo real a las pantallas de cocina (SSE)
        try {
            KitchenComandaResponse kitchenPayload = kitchenService.mapToKitchenResponse(comanda);
            kitchenRealtimeService.notifyNewComanda(restaurantId, kitchenPayload);
        } catch (Exception ignored) {
        }

        List<KardexMovementResponse> movementResponses = movements.stream()
                .map(this::mapToMovementResponse)
                .toList();

        String confirmationMessage = rejectedDishes.isEmpty()
                ? "Procesamiento de comanda confirmado exitosamente"
                : "Comanda enviada a cocina con " + validDetails.size() + " platillo(s). Se rechazaron " + rejectedDishes.size() + " platillo(s) por falta de stock.";

        return new ComandaInventoryProcessResponse(
                confirmationMessage,
                mapToComandaResponse(comanda),
                movementResponses,
                rejectedDishes
        );
    }

    /**
     * Envía a cocina la comanda pendiente en borrador de la cuenta especificada.
     *
     * @param accountId Identificador de la cuenta
     * @param authentication Información de autenticación
     * @return Confirmación del procesamiento y movimientos generados
     */
    @Transactional(noRollbackFor = ApiException.class)
    public ComandaInventoryProcessResponse sendComandaByAccountId(Long accountId, Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);

        if (accountId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_account_id",
                    "Cuenta no especificada",
                    "Debe indicar el identificador de la cuenta"
            );
        }

        Account account = accountRepository.findByIdAndRestaurantId(accountId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No se encontró una cuenta con el identificador " + accountId
                ));

        List<Comanda> draftComandas = comandaRepository.findDraftComandasByAccountId(account.getId());
        if (draftComandas.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "no_draft_comanda",
                    "Sin comanda pendiente",
                    "La cuenta no tiene ninguna comanda en borrador pendiente de enviar a cocina"
            );
        }

        return sendComanda(draftComandas.get(0).getId(), authentication);
    }

    /**
     * Envía a cocina la comanda pendiente en borrador de la mesa especificada.
     *
     * @param tableId Identificador de la mesa
     * @param authentication Información de autenticación
     * @return Confirmación del procesamiento y movimientos generados
     */
    @Transactional(noRollbackFor = ApiException.class)
    public ComandaInventoryProcessResponse sendComandaByTableId(Long tableId, Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);

        if (tableId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_table_id",
                    "Mesa no especificada",
                    "Debe indicar el identificador de la mesa"
            );
        }

        RestaurantTable table = tableRepository.findByIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "No se encontró una mesa con el identificador " + tableId
                ));

        Account account = accountRepository.findActiveByTableIdAndRestaurantId(table.getId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene una cuenta activa para enviar comandas"
                ));

        return sendComandaByAccountId(account.getId(), authentication);
    }

    /**
     * Consulta una comanda por su identificador.
     *
     * @param comandaId Identificador de la comanda
     * @return Información consolidada de la comanda y sus ítems
     */
    @Transactional(readOnly = true)
    public ComandaResponse getComandaById(Long comandaId) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        Comanda comanda = comandaRepository.findByIdWithDetailsAndRestaurantId(comandaId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "comanda_not_found",
                        "Comanda no encontrada",
                        "No se encontró una comanda con el identificador " + comandaId
                ));

        comandaDetailRepository.findByComandaIdWithModifiers(comandaId);

        return mapToComandaResponse(comanda);
    }

    /**
     * Consulta el avance de los platillos ordenados en el restaurante según los filtros indicados.
     *
     * @param restaurantId Identificador del restaurante
     * @param mesaId Filtro opcional por mesa
     * @param cuentaId Filtro opcional por cuenta
     * @param comandaId Filtro opcional por comanda
     * @param status Filtro opcional por estado de preparación
     * @return Listado de platillos con su detalle de avance y tiempos
     */
    @Transactional(readOnly = true)
    public List<ComandaDishProgressResponse> getDishProgress(
            Long restaurantId,
            Long mesaId,
            Long cuentaId,
            Long comandaId,
            ComandaDetailStatus status) {

        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        List<ComandaDetail> details = comandaDetailRepository.findDishProgress(
                targetRestaurantId,
                comandaId,
                cuentaId,
                mesaId,
                status
        );

        return details.stream()
                .map(d -> mapToDishProgressResponse(d, null, null))
                .toList();
    }

    /**
     * Consulta el avance de todos los platillos de una comanda específica.
     *
     * @param comandaId Identificador de la comanda
     * @param restaurantId Identificador del restaurante
     * @return Listado de platillos de la comanda con su avance
     */
    @Transactional(readOnly = true)
    public List<ComandaDishProgressResponse> getComandaDishProgress(Long comandaId, Long restaurantId) {
        if (comandaId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "comanda_id_required",
                    "Identificador requerido",
                    "El identificador de la comanda es obligatorio"
            );
        }

        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        Comanda comanda = comandaRepository.findByIdAndRestaurantId(comandaId, targetRestaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "comanda_not_found",
                        "Comanda no encontrada",
                        "No se encontró la comanda con identificador " + comandaId
                ));

        List<ComandaDetail> details = comandaDetailRepository.findByComandaIdWithModifiers(comanda.getId());
        return details.stream()
                .map(d -> mapToDishProgressResponse(d, null, null))
                .toList();
    }

    /**
     * Marca un platillo de una comanda como entregado a la mesa cuando se encuentra en estado LISTO.
     * Registra la fecha de entrega, el mesero responsable y sincroniza el estado de la comanda en tiempo real.
     *
     * @param detailId Identificador del detalle de la comanda (platillo)
     * @param request Solicitud opcional con notas de entrega
     * @param authentication Información de autenticación del usuario mesero o administrador
     * @return Detalle actualizado del platillo y confirmación de entrega
     */
    @Transactional
    public ComandaDishProgressResponse markDishAsDelivered(
            Long detailId,
            DeliverDishRequest request,
            Authentication authentication) {

        if (detailId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "detail_id_required",
                    "Identificador requerido",
                    "El identificador del platillo de la comanda es obligatorio"
            );
        }

        ComandaDetail detail = comandaDetailRepository.findByIdWithAllDetails(detailId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_detail_not_found",
                        "Platillo no encontrado",
                        "No se encontró el platillo de la comanda con identificador " + detailId
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

        // Si ya fue entregado, responder de forma idempotente
        if (currentStatus == ComandaDetailStatus.ENTREGADO) {
            return mapToDishProgressResponse(
                    detail,
                    "El platillo ya fue marcado como entregado previamente.",
                    currentStatus.name()
            );
        }

        // Validar que el platillo esté en estado LISTO
        if (currentStatus != ComandaDetailStatus.LISTO) {
            switch (currentStatus) {
                case BORRADOR -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_in_draft_state",
                        "Platillo en borrador",
                        "No se puede entregar un platillo que aún está en borrador y no ha sido enviado a cocina"
                );
                case RECIBIDO -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_not_ready",
                        "Platillo no listo",
                        "El platillo aún está en estado RECIBIDO en cocina y no ha sido preparado. Solo se pueden entregar platillos en estado LISTO."
                );
                case EN_PREPARACION -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_in_preparation",
                        "Platillo en preparación",
                        "El platillo aún está preparándose en cocina. Debe esperar a que cocina lo marque como LISTO antes de entregarlo a la mesa."
                );
                case CANCELADO -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_already_cancelled",
                        "Platillo cancelado",
                        "No se puede entregar un platillo cancelado."
                );
                case NO_DISPONIBLE -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_unavailable",
                        "Platillo no disponible",
                        "No se puede entregar un platillo marcado como no disponible por falta de insumos."
                );
                default -> throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_dish_delivery_state",
                        "Estado inválido para entrega",
                        "Solo los platillos en estado LISTO pueden ser marcados como entregados a la mesa."
                );
            }
        }

        Long responsibleUserId = resolveWaiterUserId(restaurantId, authentication);
        Instant now = Instant.now();

        detail.setStatus(ComandaDetailStatus.ENTREGADO);
        detail.setDeliveredAt(now);
        detail.setUpdatedById(responsibleUserId);

        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            String existingNotes = detail.getSpecialNotes();
            String appendNote = " [Entrega: " + request.notes().trim() + "]";
            detail.setSpecialNotes(existingNotes != null ? existingNotes + appendNote : appendNote.trim());
        }

        detail = comandaDetailRepository.save(detail);

        // Verificar si todos los platillos activos de la comanda ya están entregados
        List<ComandaDetail> allDetails = comandaDetailRepository.findByComandaIdWithModifiers(comanda.getId());
        boolean allActiveDelivered = allDetails.stream()
                .filter(d -> d.getStatus() != ComandaDetailStatus.CANCELADO && d.getStatus() != ComandaDetailStatus.NO_DISPONIBLE)
                .allMatch(d -> d.getId().equals(detailId) || d.getStatus() == ComandaDetailStatus.ENTREGADO);

        if (allActiveDelivered) {
            if (comanda.getStatus() == ComandaStatus.EN_PREPARACION) {
                comanda.setStatus(ComandaStatus.LISTA);
                comandaRepository.save(comanda);
            }
            if (comanda.getStatus() == ComandaStatus.LISTA) {
                comanda.setStatus(ComandaStatus.ENTREGADA);
                comanda.setFinishedAt(now);
                comandaRepository.save(comanda);
            }
        }

        String message = "Platillo '" + detail.getNameSnapshot() + "' marcado como entregado exitosamente a la mesa.";
        ComandaDishProgressResponse response = mapToDishProgressResponse(detail, message, currentStatus.name());

        // Notificar en tiempo real a cocina y meseros
        try {
            kitchenRealtimeService.notifyDishDelivered(restaurantId, response);
            KitchenComandaResponse kitchenPayload = kitchenService.getKitchenComandaById(comanda.getId(), restaurantId);
            kitchenRealtimeService.notifyComandaUpdated(restaurantId, kitchenPayload);
        } catch (Exception ignored) {
        }

        return response;
    }

    private ComandaDishProgressResponse mapToDishProgressResponse(
            ComandaDetail detail,
            String message,
            String previousStatus) {

        Comanda comanda = detail.getComanda();
        Long comandaId = comanda != null ? comanda.getId() : null;
        Long accountId = (comanda != null && comanda.getAccount() != null) ? comanda.getAccount().getId() : null;
        Long tableId = (comanda != null && comanda.getAccount() != null) ? comanda.getAccount().getTableId() : null;
        String tableNumber = comanda != null ? resolveTableNumber(comanda) : "";
        short roundNumber = comanda != null ? comanda.getRoundNumber() : 1;
        Long waiterId = comanda != null ? comanda.getWaiterId() : null;
        String waiterName = comanda != null ? resolveWaiterName(comanda) : "Mesero no asignado";
        String comandaStatus = comanda != null && comanda.getStatus() != null ? comanda.getStatus().name() : "";

        List<String> modifiers = new ArrayList<>();
        if (detail.getModifiers() != null) {
            for (ComandaDetailModifier m : detail.getModifiers()) {
                if (m.getNameSnapshot() != null) {
                    modifiers.add(m.getNameSnapshot());
                } else if (m.getModifier() != null) {
                    modifiers.add(m.getModifier().getName());
                }
            }
        }

        return new ComandaDishProgressResponse(
                detail.getId(),
                comandaId,
                accountId,
                tableId,
                tableNumber,
                roundNumber,
                detail.getDish() != null ? detail.getDish().getId() : null,
                detail.getCombo() != null ? detail.getCombo().getId() : null,
                detail.getNameSnapshot(),
                detail.getQuantity(),
                detail.getUnitPriceSnapshot(),
                detail.getStatus().name(),
                previousStatus != null ? previousStatus : detail.getStatus().name(),
                detail.getSpecialNotes(),
                modifiers,
                detail.getReceivedAt(),
                detail.getPreparationStartedAt(),
                detail.getReadyAt(),
                detail.getDeliveredAt(),
                waiterId,
                waiterName,
                comandaStatus,
                message
        );
    }

    private String resolveTableNumber(Comanda comanda) {
        if (comanda.getAccount() != null && comanda.getAccount().getTableId() != null) {
            return tableRepository.findById(comanda.getAccount().getTableId())
                    .map(RestaurantTable::getNumber)
                    .orElse("");
        }
        return "";
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

    /**
     * Registra una ronda adicional de platillos en una cuenta abierta.
     * Cada ronda cuenta con un ciclo de vida de estados independiente (BORRADOR -> RECIBIDA -> EN_PREPARACION -> LISTA -> ENTREGADA).
     *
     * @param accountId Identificador de la cuenta abierta
     * @param request Datos de la nueva ronda y sus ítems
     * @param authentication Información de autenticación del operador
     * @return Detalle de la nueva ronda creada y sus ítems
     */
    @Transactional
    public AccountRoundResponse createAccountRound(
            Long accountId,
            CreateAccountRoundRequest request,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        Long waiterId = resolveWaiterUserId(restaurantId, authentication);

        if (accountId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_account_id",
                    "Cuenta no especificada",
                    "Debe indicar el identificador de la cuenta"
            );
        }

        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "empty_round",
                    "Ronda vacía",
                    "La nueva ronda debe contener al menos un platillo o combo"
            );
        }

        Account account = accountRepository.findByIdAndRestaurantId(accountId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No se encontró una cuenta con el identificador " + accountId
                ));

        if ("LISTA_COBRO".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_already_in_billing",
                    "Cuenta en proceso de cobro",
                    "No se pueden agregar rondas a una cuenta que ya está en proceso de cobro"
            );
        }

        if (!"ABIERTA".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta inválido",
                    "Solo se pueden agregar rondas a cuentas en estado ABIERTA"
            );
        }

        // Si existe un borrador no enviado, no permitir crear otra ronda en borrador hasta enviar la previa
        List<Comanda> draftComandas = comandaRepository.findDraftComandasByAccountId(account.getId());
        if (!draftComandas.isEmpty() && !Boolean.TRUE.equals(request.sendImmediately())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "existing_draft_comanda",
                    "Comanda en borrador pendiente",
                    "La cuenta ya tiene la ronda #" + draftComandas.get(0).getRoundNumber() + " en borrador pendiente de enviar a cocina. Envíe la comanda pendiente antes de registrar otra ronda en borrador."
            );
        }

        short nextRound = (short) (comandaRepository.findMaxRoundNumberByAccountId(account.getId()) + 1);
        Comanda comanda = new Comanda(
                account,
                nextRound,
                waiterId,
                request.generalNotes() != null ? request.generalNotes().trim() : null
        );
        Comanda savedComanda = comandaRepository.save(comanda);

        processItemsForComanda(restaurantId, savedComanda, request.items());

        validateSupplyAvailability(restaurantId, savedComanda);

        boolean sent = false;
        if (Boolean.TRUE.equals(request.sendImmediately())) {
            sendComanda(savedComanda.getId(), authentication);
            sent = true;
        }

        Comanda updatedComanda = comandaRepository.findByIdWithDetailsAndRestaurantId(savedComanda.getId(), restaurantId)
                .orElse(savedComanda);
        comandaDetailRepository.findByComandaIdWithModifiers(updatedComanda.getId());

        int totalRounds = comandaRepository.findByAccountId(account.getId()).size();
        String message = sent
                ? "Ronda #" + nextRound + " registrada y enviada a cocina exitosamente con descuento de inventario."
                : "Ronda #" + nextRound + " registrada exitosamente en borrador.";

        return mapToAccountRoundResponse(updatedComanda, totalRounds, message);
    }

    /**
     * Consulta todas las rondas de comandas asociadas a una cuenta abierta utilizando la autenticación del usuario.
     *
     * @param accountId Identificador de la cuenta
     * @param authentication Información de autenticación del operador
     * @return Listado de todas las rondas con sus estados independientes
     */
    @Transactional(readOnly = true)
    public List<AccountRoundResponse> getAccountRounds(Long accountId, Authentication authentication) {
        Long restaurantId = resolveRestaurantId(authentication);
        return getAccountRounds(accountId, restaurantId);
    }

    /**
     * Consulta todas las rondas de comandas asociadas a una cuenta abierta.
     *
     * @param accountId Identificador de la cuenta
     * @param restaurantId Identificador del restaurante
     * @return Listado de todas las rondas con sus estados independientes
     */
    @Transactional(readOnly = true)
    public List<AccountRoundResponse> getAccountRounds(Long accountId, Long restaurantId) {
        if (accountId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_account_id",
                    "Cuenta no especificada",
                    "Debe indicar el identificador de la cuenta"
            );
        }

        Long targetRestaurantId = restaurantId != null ? restaurantId : DEFAULT_RESTAURANT_ID;
        Account account = accountRepository.findByIdAndRestaurantId(accountId, targetRestaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No se encontró una cuenta con el identificador " + accountId
                ));

        List<Comanda> comandas = comandaRepository.findByAccountId(account.getId());
        if (comandas.isEmpty()) {
            return List.of();
        }

        List<Long> comandaIds = comandas.stream().map(Comanda::getId).toList();
        comandaDetailRepository.findByComandaIdInWithModifiers(comandaIds);

        int totalRounds = comandas.size();
        return comandas.stream()
                .map(c -> mapToAccountRoundResponse(c, totalRounds, null))
                .toList();
    }

    private AccountRoundResponse mapToAccountRoundResponse(Comanda comanda, int totalRounds, String message) {
        List<ComandaItemResponse> itemResponses = new ArrayList<>();
        if (comanda.getDetails() != null) {
            for (ComandaDetail detail : comanda.getDetails()) {
                List<String> modNames = new ArrayList<>();
                if (detail.getModifiers() != null) {
                    for (ComandaDetailModifier cdm : detail.getModifiers()) {
                        modNames.add(cdm.getNameSnapshot());
                    }
                }

                itemResponses.add(new ComandaItemResponse(
                        detail.getId(),
                        detail.getDish() != null ? detail.getDish().getId() : null,
                        detail.getCombo() != null ? detail.getCombo().getId() : null,
                        detail.getNameSnapshot(),
                        detail.getQuantity(),
                        detail.getUnitPriceSnapshot(),
                        detail.getStatus().name(),
                        detail.getSpecialNotes(),
                        modNames
                ));
            }
        }

        Long accountId = comanda.getAccount() != null ? comanda.getAccount().getId() : null;
        Long tableId = comanda.getAccount() != null ? comanda.getAccount().getTableId() : null;
        String tableNumber = resolveTableNumber(comanda);
        String waiterName = resolveWaiterName(comanda);

        return new AccountRoundResponse(
                comanda.getRoundNumber(),
                comanda.getId(),
                accountId,
                tableId,
                tableNumber,
                comanda.getStatus().name(),
                comanda.getGeneralNotes(),
                comanda.getWaiterId(),
                waiterName,
                comanda.getCreatedAt(),
                comanda.getSentAt(),
                comanda.getFinishedAt(),
                itemResponses,
                totalRounds,
                message
        );
    }

    private void processItemsForComanda(Long restaurantId, Comanda savedComanda, List<CreateComandaItemRequest> items) {
        for (CreateComandaItemRequest item : items) {
            if (item.dishId() == null && item.comboId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_product",
                        "Producto no especificado",
                        "Debe especificar un platillo o un combo para cada ítem de la comanda"
                );
            }
            if (item.dishId() != null && item.comboId() != null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "ambiguous_product",
                        "Producto ambiguo",
                        "Un ítem no puede ser simultáneamente un platillo y un combo"
                );
            }
            if (item.quantity() <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_item_quantity",
                        "Cantidad inválida",
                        "La cantidad ordenada debe ser mayor a cero"
                );
            }

            if (item.dishId() != null) {
                Dish dish = dishRepository.findByIdAndRestaurantId(item.dishId(), restaurantId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "dish_not_found",
                                "Platillo no encontrado",
                                "No se encontró un platillo con el identificador " + item.dishId()
                        ));

                if (!dish.isActive() || !dish.isManualAvailable()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "dish_not_available",
                            "Platillo no disponible",
                            "El platillo '" + dish.getName() + "' no se encuentra disponible en el menú"
                    );
                }

                RecipeVersion recipeVersion = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "recipe_not_valid",
                                "Receta no vigente",
                                "El platillo '" + dish.getName() + "' no cuenta con una receta vigente"
                        ));

                ComandaDetail detail = new ComandaDetail(
                        savedComanda,
                        dish,
                        recipeVersion,
                        dish.getName(),
                        item.quantity(),
                        dish.getSalePrice(),
                        BigDecimal.ZERO,
                        dish.getPreparationTimeMinutes() != null ? dish.getPreparationTimeMinutes() : (short) 15,
                        item.specialNotes() != null ? item.specialNotes().trim() : null
                );

                if (item.modifierIds() != null && !item.modifierIds().isEmpty()) {
                    for (Long modId : item.modifierIds()) {
                        Modifier modifier = modifierRepository.findById(modId)
                                .orElseThrow(() -> new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "modifier_not_found",
                                        "Modificador no encontrado",
                                        "No se encontró un modificador con el identificador " + modId
                                ));

                        if (!modifier.isActive()) {
                            throw new ApiException(
                                    HttpStatus.BAD_REQUEST,
                                    "modifier_inactive",
                                    "Modificador inactivo",
                                    "El modificador '" + modifier.getName() + "' no se encuentra activo"
                            );
                        }

                        ModifierRecipeVersion mrv = modifierRecipeVersionRepository.findActiveByModifierId(modifier.getId())
                                .orElse(null);

                        ComandaDetailModifier cdm = new ComandaDetailModifier(
                                detail,
                                modifier,
                                mrv,
                                modifier.getName(),
                                (short) 1,
                                modifier.getAdditionalPrice() != null ? modifier.getAdditionalPrice() : BigDecimal.ZERO,
                                BigDecimal.ZERO
                        );
                        detail.addModifier(cdm);
                    }
                }

                savedComanda.addDetail(detail);
                comandaDetailRepository.save(detail);

            } else {
                Combo combo = comboRepository.findByIdAndRestaurantId(item.comboId(), restaurantId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "combo_not_found",
                                "Combo no encontrado",
                                "No se encontró un combo con el identificador " + item.comboId()
                        ));

                if (!combo.isActive() || !combo.isManualAvailable()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "combo_not_available",
                            "Combo no disponible",
                            "El combo '" + combo.getName() + "' no se encuentra disponible en el menú"
                    );
                }

                ComandaDetail detail = new ComandaDetail(
                        savedComanda,
                        combo,
                        combo.getName(),
                        item.quantity(),
                        combo.getSalePrice(),
                        BigDecimal.ZERO,
                        combo.getPreparationTimeMinutes() != null ? combo.getPreparationTimeMinutes() : (short) 20,
                        item.specialNotes() != null ? item.specialNotes().trim() : null
                );

                savedComanda.addDetail(detail);
                comandaDetailRepository.save(detail);
            }
        }
    }

    private Map<Long, BigDecimal> calculateSupplyRequirements(Long restaurantId, Comanda comanda) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        if (comanda.getDetails() != null) {
            for (ComandaDetail detail : comanda.getDetails()) {
                Map<Long, BigDecimal> detailTotals = calculateDetailSupplyRequirements(restaurantId, detail);
                for (Map.Entry<Long, BigDecimal> entry : detailTotals.entrySet()) {
                    totals.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
                }
            }
        }
        return totals;
    }

    private Map<Long, BigDecimal> calculateDetailSupplyRequirements(Long restaurantId, ComandaDetail detail) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();

        if (detail.getDish() != null) {
            Dish dish = detail.getDish();
            RecipeVersion recipe = detail.getRecipeVersion();
            if (recipe == null) {
                recipe = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "recipe_not_valid",
                                "Receta no vigente",
                                "El platillo '" + dish.getName() + "' no cuenta con una receta vigente"
                        ));
            }

            for (RecipeDetail rd : recipe.getDetails()) {
                Supply supply = rd.getSupply();
                BigDecimal urBase = rd.getMeasurementUnit() != null ? rd.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;
                BigDecimal usBase = supply.getMeasurementUnit() != null ? supply.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;

                BigDecimal required = BigDecimal.valueOf(detail.getQuantity())
                        .multiply(rd.getQuantity())
                        .multiply(urBase)
                        .divide(usBase, 4, RoundingMode.HALF_UP);

                totals.merge(supply.getId(), required, BigDecimal::add);
            }

            // Modificadores asociados al platillo
            if (detail.getModifiers() != null) {
                for (ComandaDetailModifier cdm : detail.getModifiers()) {
                    Modifier modifier = cdm.getModifier();
                    ModifierRecipeVersion mrv = cdm.getModifierRecipeVersion();
                    if (mrv == null) {
                        mrv = modifierRecipeVersionRepository.findActiveByModifierId(modifier.getId()).orElse(null);
                    }

                    if (mrv != null) {
                        List<ModifierRecipeDetail> mrds = modifierRecipeDetailRepository.findByModifierRecipeVersionId(mrv.getId());
                        for (ModifierRecipeDetail mrd : mrds) {
                            Supply supply = mrd.getSupply();
                            BigDecimal urBase = mrd.getMeasurementUnit() != null ? mrd.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;
                            BigDecimal usBase = supply.getMeasurementUnit() != null ? supply.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;

                            BigDecimal modQty = BigDecimal.valueOf(detail.getQuantity())
                                    .multiply(BigDecimal.valueOf(cdm.getQuantity()))
                                    .multiply(mrd.getQuantity())
                                    .multiply(urBase)
                                    .divide(usBase, 4, RoundingMode.HALF_UP);

                            if ("AGREGAR".equalsIgnoreCase(mrd.getAdjustmentType())) {
                                totals.merge(supply.getId(), modQty, BigDecimal::add);
                            } else {
                                // Omisión / reducción
                                totals.merge(supply.getId(), modQty.negate(), BigDecimal::add);
                            }
                        }
                    }
                }
            }
        } else if (detail.getCombo() != null) {
            Combo combo = detail.getCombo();
            List<ComboDetail> comboItems = comboDetailRepository.findByComboIdWithDish(combo.getId());

            for (ComboDetail cd : comboItems) {
                Dish dish = cd.getDish();
                RecipeVersion recipe = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "recipe_not_valid",
                                "Receta no vigente",
                                "El platillo '" + dish.getName() + "' del combo no cuenta con una receta vigente"
                        ));

                for (RecipeDetail rd : recipe.getDetails()) {
                    Supply supply = rd.getSupply();
                    BigDecimal urBase = rd.getMeasurementUnit() != null ? rd.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;
                    BigDecimal usBase = supply.getMeasurementUnit() != null ? supply.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;

                    BigDecimal required = BigDecimal.valueOf(detail.getQuantity())
                            .multiply(BigDecimal.valueOf(cd.getQuantity()))
                            .multiply(rd.getQuantity())
                            .multiply(urBase)
                            .divide(usBase, 4, RoundingMode.HALF_UP);

                    totals.merge(supply.getId(), required, BigDecimal::add);
                }
            }
        }

        // El consumo neto no puede ser negativo
        totals.replaceAll((id, qty) -> qty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : qty);

        return totals;
    }

    private Account resolveOrCreateAccount(Long restaurantId, CreateComandaRequest request, Long waiterId) {
        if (request.accountId() != null) {
            return accountRepository.findByIdAndRestaurantId(request.accountId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "account_not_found",
                            "Cuenta no encontrada",
                            "No se encontró una cuenta con el identificador " + request.accountId()
                    ));
        }

        Long tableId = request.tableId() != null ? request.tableId() : 1L;
        RestaurantTable table = tableRepository.findByIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "No se encontró una mesa con el identificador " + tableId
                ));

        return accountRepository.findActiveByTableIdAndRestaurantId(tableId, restaurantId)
                .orElseGet(() -> {
                    String accountNumber = "CTA-" + table.getNumber() + "-" + (System.currentTimeMillis() % 100000);
                    Account newAccount = new Account(restaurantId, table.getId(), waiterId, accountNumber, (short) 2);
                    return accountRepository.save(newAccount);
                });
    }

    private Long resolveWaiterUserId(Long restaurantId, Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            Long userId = jwtData.userId();
            if (jwtData.role() == RoleName.WAITER) {
                return userId;
            }
        }

        List<Long> waiterIds = userProfileRepository.findActiveWaitersByRestaurantId(restaurantId);
        if (!waiterIds.isEmpty()) {
            return waiterIds.get(0);
        }

        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.userId();
        }

        return 1L;
    }

    private ComandaResponse mapToComandaResponse(Comanda comanda) {
        List<ComandaItemResponse> itemResponses = new ArrayList<>();
        if (comanda.getDetails() != null) {
            for (ComandaDetail detail : comanda.getDetails()) {
                List<String> modNames = new ArrayList<>();
                if (detail.getModifiers() != null) {
                    for (ComandaDetailModifier cdm : detail.getModifiers()) {
                        modNames.add(cdm.getNameSnapshot());
                    }
                }

                itemResponses.add(new ComandaItemResponse(
                        detail.getId(),
                        detail.getDish() != null ? detail.getDish().getId() : null,
                        detail.getCombo() != null ? detail.getCombo().getId() : null,
                        detail.getNameSnapshot(),
                        detail.getQuantity(),
                        detail.getUnitPriceSnapshot(),
                        detail.getStatus().name(),
                        detail.getSpecialNotes(),
                        modNames
                ));
            }
        }

        return new ComandaResponse(
                comanda.getId(),
                comanda.getAccount() != null ? comanda.getAccount().getId() : null,
                comanda.getAccount() != null ? comanda.getAccount().getTableId() : null,
                comanda.getRoundNumber(),
                comanda.getWaiterId(),
                comanda.getStatus().name(),
                comanda.getGeneralNotes(),
                comanda.getCreatedAt(),
                comanda.getSentAt(),
                itemResponses
        );
    }

    private KardexMovementResponse mapToMovementResponse(InventoryMovement movement) {
        Supply supply = movement.getSupply();
        String unit = (supply != null && supply.getMeasurementUnit() != null)
                ? supply.getMeasurementUnit().getAbbreviation()
                : "u";

        return new KardexMovementResponse(
                movement.getId(),
                supply != null ? supply.getId() : null,
                supply != null ? supply.getCode() : null,
                supply != null ? supply.getName() : null,
                unit,
                movement.getType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getResultingStock(),
                movement.getReason(),
                movement.getResponsibleUserId(),
                movement.getCreatedAt()
        );
    }

    /**
     * Agrega platillos a una cuenta abierta indicando cantidad, modificadores y notas.
     * Valida que la cuenta se encuentre abierta y que exista stock suficiente de todos los insumos de su receta.
     */
    @Transactional
    public AddDishResponse addDishesToAccount(
            Long accountId,
            AddDishToAccountRequest request,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        Long waiterId = resolveWaiterUserId(restaurantId, authentication);

        if (accountId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_account_id",
                    "Cuenta no especificada",
                    "Debe indicar el identificador de la cuenta"
            );
        }

        Account account = accountRepository.findByIdAndRestaurantId(accountId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No se encontró una cuenta con el identificador " + accountId
                ));

        if ("LISTA_COBRO".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_already_in_billing",
                    "Cuenta en proceso de cobro",
                    "No se pueden agregar platillos a una cuenta que ya está en proceso de cobro"
            );
        }

        if (!"ABIERTA".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta inválido",
                    "Solo se pueden agregar platillos a cuentas en estado abierta"
            );
        }

        List<AddDishItemRequest> items = extractItems(request);
        if (items.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish",
                    "Platillo no especificado",
                    "Debe indicar al menos un platillo para agregar a la cuenta"
            );
        }

        List<PreparedItemData> preparedItems = new ArrayList<>();
        Map<Long, BigDecimal> supplyRequirements = new LinkedHashMap<>();
        Map<Long, String> supplyDishNames = new LinkedHashMap<>();

        for (AddDishItemRequest item : items) {
            if (item.dishId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_dish_id",
                        "Platillo no especificado",
                        "El identificador del platillo es obligatorio"
                );
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_item_quantity",
                        "Cantidad inválida",
                        "La cantidad ordenada debe ser mayor a cero"
                );
            }

            Dish dish = dishRepository.findByIdAndRestaurantId(item.dishId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "dish_not_found",
                            "Platillo no encontrado",
                            "No se encontró un platillo con el identificador " + item.dishId()
                    ));

            if (!dish.isActive() || !dish.isManualAvailable()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "dish_not_available",
                        "Platillo no disponible",
                        "El platillo '" + dish.getName() + "' no se encuentra disponible en el menú"
                );
            }

            RecipeVersion recipe = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "recipe_not_valid",
                            "Receta no vigente",
                            "El platillo '" + dish.getName() + "' no cuenta con una receta vigente"
                    ));

            List<PreparedModifierData> preparedMods = new ArrayList<>();
            if (item.modifierIds() != null && !item.modifierIds().isEmpty()) {
                for (Long modId : item.modifierIds()) {
                    Modifier mod = modifierRepository.findById(modId)
                            .orElseThrow(() -> new ApiException(
                                    HttpStatus.NOT_FOUND,
                                    "modifier_not_found",
                                    "Modificador no encontrado",
                                    "No se encontró un modificador con el identificador " + modId
                            ));

                    if (!mod.isActive()) {
                        throw new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "modifier_inactive",
                                "Modificador inactivo",
                                "El modificador '" + mod.getName() + "' no se encuentra activo"
                        );
                    }

                    ModifierRecipeVersion mrv = modifierRecipeVersionRepository.findActiveByModifierId(mod.getId()).orElse(null);
                    preparedMods.add(new PreparedModifierData(mod, mrv));
                }
            }

            preparedItems.add(new PreparedItemData(dish, recipe, item.quantity(), item.notes(), preparedMods));

            for (RecipeDetail rd : recipe.getDetails()) {
                Supply supply = rd.getSupply();
                BigDecimal urBase = rd.getMeasurementUnit() != null ? rd.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;
                BigDecimal usBase = supply.getMeasurementUnit() != null ? supply.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;

                BigDecimal required = BigDecimal.valueOf(item.quantity())
                        .multiply(rd.getQuantity())
                        .multiply(urBase)
                        .divide(usBase, 4, RoundingMode.HALF_UP);

                supplyRequirements.merge(supply.getId(), required, BigDecimal::add);
                supplyDishNames.putIfAbsent(supply.getId(), dish.getName());
            }

            for (PreparedModifierData pmod : preparedMods) {
                if (pmod.recipeVersion() != null) {
                    List<ModifierRecipeDetail> mrds = modifierRecipeDetailRepository.findByModifierRecipeVersionId(pmod.recipeVersion().getId());
                    for (ModifierRecipeDetail mrd : mrds) {
                        Supply supply = mrd.getSupply();
                        BigDecimal urBase = mrd.getMeasurementUnit() != null ? mrd.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;
                        BigDecimal usBase = supply.getMeasurementUnit() != null ? supply.getMeasurementUnit().getBaseFactor() : BigDecimal.ONE;

                        BigDecimal modQty = BigDecimal.valueOf(item.quantity())
                                .multiply(BigDecimal.ONE)
                                .multiply(mrd.getQuantity())
                                .multiply(urBase)
                                .divide(usBase, 4, RoundingMode.HALF_UP);

                        if ("AGREGAR".equalsIgnoreCase(mrd.getAdjustmentType())) {
                            supplyRequirements.merge(supply.getId(), modQty, BigDecimal::add);
                            supplyDishNames.putIfAbsent(supply.getId(), dish.getName());
                        } else {
                            supplyRequirements.merge(supply.getId(), modQty.negate(), BigDecimal::add);
                        }
                    }
                }
            }
        }

        for (Map.Entry<Long, BigDecimal> entry : supplyRequirements.entrySet()) {
            BigDecimal requiredAmount = entry.getValue();
            if (requiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                Supply supply = supplyRepository.findByIdAndRestaurantId(entry.getKey(), restaurantId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "supply_not_found",
                                "Insumo no encontrado",
                                "No se encontró el insumo con identificador " + entry.getKey()
                        ));

                if (!supply.isActive()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "inactive_supply",
                            "Insumo inactivo",
                            "El platillo requiere el insumo inactivo '" + supply.getName() + "'"
                    );
                }

                if (supply.getCurrentStock().compareTo(requiredAmount) < 0) {
                    String dishName = supplyDishNames.getOrDefault(supply.getId(), "seleccionado");
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "insufficient_supply_stock",
                            "Stock insuficiente de insumos",
                            "El platillo '" + dishName + "' no tiene stock suficiente del insumo '" + supply.getName() + "' (requerido: " + requiredAmount + ", disponible: " + supply.getCurrentStock() + ")"
                    );
                }
            }
        }

        List<Comanda> draftComandas = comandaRepository.findDraftComandasByAccountId(account.getId());
        Comanda targetComanda;
        if (!draftComandas.isEmpty()) {
            targetComanda = draftComandas.get(0);
        } else {
            short nextRound = (short) (comandaRepository.findMaxRoundNumberByAccountId(account.getId()) + 1);
            targetComanda = new Comanda(account, nextRound, waiterId, null);
            targetComanda = comandaRepository.save(targetComanda);
        }

        List<DishOrderDetailResponse> registeredDishes = new ArrayList<>();
        BigDecimal subtotalAgregado = BigDecimal.ZERO;

        for (PreparedItemData item : preparedItems) {
            Dish dish = item.dish();
            RecipeVersion recipe = item.recipe();
            short qty = item.quantity();
            String notes = item.notes() != null ? item.notes().trim() : null;

            ComandaDetail detail = new ComandaDetail(
                    targetComanda,
                    dish,
                    recipe,
                    dish.getName(),
                    qty,
                    dish.getSalePrice(),
                    BigDecimal.ZERO,
                    dish.getPreparationTimeMinutes() != null ? dish.getPreparationTimeMinutes() : (short) 15,
                    notes
            );
            detail.setStatus(ComandaDetailStatus.BORRADOR);

            List<String> modNames = new ArrayList<>();
            BigDecimal modTotalUnit = BigDecimal.ZERO;

            for (PreparedModifierData pmod : item.modifiers()) {
                Modifier mod = pmod.modifier();
                BigDecimal modPrice = mod.getAdditionalPrice() != null ? mod.getAdditionalPrice() : BigDecimal.ZERO;
                ComandaDetailModifier cdm = new ComandaDetailModifier(
                        detail,
                        mod,
                        pmod.recipeVersion(),
                        mod.getName(),
                        (short) 1,
                        modPrice,
                        BigDecimal.ZERO
                );
                detail.addModifier(cdm);
                modNames.add(mod.getName());
                modTotalUnit = modTotalUnit.add(modPrice);
            }

            targetComanda.addDetail(detail);
            ComandaDetail savedDetail = comandaDetailRepository.save(detail);

            BigDecimal lineSubtotal = dish.getSalePrice().add(modTotalUnit).multiply(BigDecimal.valueOf(qty));
            subtotalAgregado = subtotalAgregado.add(lineSubtotal);

            registeredDishes.add(new DishOrderDetailResponse(
                    savedDetail.getId(),
                    dish.getId(),
                    dish.getName(),
                    qty,
                    dish.getSalePrice(),
                    lineSubtotal,
                    notes,
                    modNames,
                    savedDetail.getStatus().name()
            ));
        }

        RestaurantTable table = tableRepository.findByIdAndRestaurantId(account.getTableId(), restaurantId).orElse(null);
        String tableNumber = table != null ? table.getNumber() : "";

        return new AddDishResponse(
                "Platillo(s) registrado(s) exitosamente en la cuenta",
                account.getId(),
                account.getAccountNumber(),
                table != null ? table.getId() : null,
                tableNumber,
                targetComanda.getId(),
                targetComanda.getRoundNumber(),
                registeredDishes,
                subtotalAgregado
        );
    }

    /**
     * Agrega platillos a la cuenta activa de la mesa indicada.
     */
    @Transactional
    public AddDishResponse addDishesToTable(
            Long tableId,
            AddDishToAccountRequest request,
            Authentication authentication) {

        Long restaurantId = resolveRestaurantId(authentication);
        if (tableId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_table_id",
                    "Mesa no especificada",
                    "Debe indicar el identificador de la mesa"
            );
        }

        RestaurantTable table = tableRepository.findByIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "No se encontró una mesa con el identificador " + tableId
                ));

        Account account = accountRepository.findActiveByTableIdAndRestaurantId(table.getId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene una cuenta activa para agregar platillos"
                ));

        return addDishesToAccount(account.getId(), request, authentication);
    }

    public void validateSupplyAvailability(Long restaurantId, Comanda comanda) {
        Map<Long, BigDecimal> supplyTotals = calculateSupplyRequirements(restaurantId, comanda);

        for (Map.Entry<Long, BigDecimal> entry : supplyTotals.entrySet()) {
            Long supplyId = entry.getKey();
            BigDecimal requiredAmount = entry.getValue();

            if (requiredAmount.compareTo(BigDecimal.ZERO) > 0) {
                Supply supply = supplyRepository.findByIdAndRestaurantId(supplyId, restaurantId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "supply_not_found",
                                "Insumo no encontrado",
                                "No se encontró el insumo con identificador " + supplyId
                        ));

                if (!supply.isActive()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "inactive_supply",
                            "Insumo inactivo",
                            "La comanda requiere el insumo inactivo '" + supply.getName() + "'"
                    );
                }

                if (supply.getCurrentStock().compareTo(requiredAmount) < 0) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "insufficient_supply_stock",
                            "Stock insuficiente de insumos",
                            "No fue posible registrar los platillos: stock insuficiente del insumo '" + supply.getName() + "' (requerido: " + requiredAmount + ", disponible: " + supply.getCurrentStock() + ")"
                    );
                }
            }
        }
    }

    private Long resolveRestaurantId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return userProfileRepository.findById(jwtData.userId())
                    .map(RestaurantUserProfile::getRestaurantId)
                    .orElse(DEFAULT_RESTAURANT_ID);
        }
        return DEFAULT_RESTAURANT_ID;
    }

    private List<AddDishItemRequest> extractItems(AddDishToAccountRequest request) {
        if (request == null) {
            return List.of();
        }
        if (request.items() != null && !request.items().isEmpty()) {
            return request.items();
        }
        if (request.dishId() != null) {
            short qty = (request.quantity() != null && request.quantity() > 0) ? request.quantity() : 1;
            return List.of(new AddDishItemRequest(request.dishId(), qty, request.notes(), request.modifierIds()));
        }
        return List.of();
    }

    private record PreparedModifierData(Modifier modifier, ModifierRecipeVersion recipeVersion) {
    }

    private record PreparedItemData(Dish dish, RecipeVersion recipe, short quantity, String notes, List<PreparedModifierData> modifiers) {
    }
}
