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
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.ComboDetailRepository;
import com.restaurante.domain.repository.ComboRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.InventoryMovementRepository;
import com.restaurante.domain.repository.ModifierRecipeDetailRepository;
import com.restaurante.domain.repository.ModifierRecipeVersionRepository;
import com.restaurante.domain.repository.ModifierRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.comanda.ComandaInventoryProcessResponse;
import com.restaurante.web.dto.comanda.ComandaItemResponse;
import com.restaurante.web.dto.comanda.ComandaResponse;
import com.restaurante.web.dto.comanda.CreateComandaItemRequest;
import com.restaurante.web.dto.comanda.CreateComandaRequest;
import com.restaurante.web.dto.comanda.KardexMovementResponse;
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
                                   RestaurantUserProfileRepository userProfileRepository) {
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

        short nextRound = (short) (comandaRepository.findMaxRoundNumberByAccountId(account.getId()) + 1);
        Comanda comanda = new Comanda(
                account,
                nextRound,
                waiterId,
                request.generalNotes() != null ? request.generalNotes().trim() : null
        );
        Comanda savedComanda = comandaRepository.save(comanda);

        for (CreateComandaItemRequest item : request.items()) {
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
     *
     * @param comandaId Identificador de la comanda a enviar
     * @param authentication Información de autenticación del usuario que procesa el envío
     * @return Confirmación del procesamiento, estado actualizado y movimientos de kardex generados
     */
    @Transactional
    public ComandaInventoryProcessResponse sendComanda(Long comandaId, Authentication authentication) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

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

        // Calcular consumos consolidados de insumos (recetas base, combos y modificadores)
        Map<Long, BigDecimal> supplyTotals = calculateSupplyRequirements(restaurantId, comanda);

        // Pre-validar existencia de stock suficiente para evitar errores parciales
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
                            "insufficient_inventory",
                            "Inventario insuficiente",
                            "No fue posible actualizar el inventario: stock insuficiente para preparar los platillos solicitados"
                    );
                }
            }
        }

        // Transición de estado a RECIBIDA (lo que detona el trigger de BD en PostgreSQL)
        comanda.setStatus(ComandaStatus.RECIBIDA);
        comanda.setSentAt(Instant.now());

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

        // Obtener los movimientos registrados en el kardex
        List<InventoryMovement> movements = inventoryMovementRepository.findByComandaIdWithSupply(comanda.getId());

        if (movements.isEmpty()) {
            // Mecanismo de respaldo para entornos de pruebas en memoria donde los triggers de base de datos no estén activos
            for (Map.Entry<Long, BigDecimal> entry : supplyTotals.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    Supply supply = supplyRepository.findById(entry.getKey()).orElseThrow();
                    BigDecimal prevStock = supply.getCurrentStock();
                    BigDecimal newStock = prevStock.subtract(entry.getValue());
                    supply.setCurrentStock(newStock);
                    supplyRepository.save(supply);

                    ComandaDetail firstDetail = comanda.getDetails().get(0);
                    InventoryMovement movement = new InventoryMovement(
                            restaurantId,
                            supply,
                            "SALIDA_VENTA",
                            entry.getValue(),
                            firstDetail,
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
            for (ComandaDetail detail : comanda.getDetails()) {
                detail.setStatus(ComandaDetailStatus.RECIBIDO);
                detail.setReceivedAt(comanda.getSentAt());
            }
        } else {
            // Sincronizar entidades de insumos afectadas en el contexto de persistencia
            for (InventoryMovement movement : movements) {
                entityManager.refresh(movement.getSupply());
            }
        }

        List<KardexMovementResponse> movementResponses = movements.stream()
                .map(this::mapToMovementResponse)
                .toList();

        return new ComandaInventoryProcessResponse(
                "Procesamiento de comanda confirmado exitosamente",
                mapToComandaResponse(comanda),
                movementResponses
        );
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

    private Map<Long, BigDecimal> calculateSupplyRequirements(Long restaurantId, Comanda comanda) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();

        for (ComandaDetail detail : comanda.getDetails()) {
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
}
