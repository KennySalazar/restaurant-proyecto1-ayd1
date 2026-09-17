package com.restaurante.application.inventory;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.InventoryMovement;
import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.RecipeDetail;
import com.restaurante.domain.model.RecipeVersion;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.InventoryMovementRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.inventory.AffectedDishResponse;
import com.restaurante.web.dto.inventory.CreateInventoryAdjustmentRequest;
import com.restaurante.web.dto.inventory.InventoryAdjustmentResponse;
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
import java.util.List;

/**
 * Servicio de aplicación para el registro y gestión de ajustes manuales de inventario.
 */
@Service
public class InventoryAdjustmentService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final SupplyRepository supplyRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final RestaurantUserProfileRepository userProfileRepository;
    private final NotificationRepository notificationRepository;
    private final RecipeVersionRepository recipeVersionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public InventoryAdjustmentService(SupplyRepository supplyRepository,
                                      InventoryMovementRepository inventoryMovementRepository,
                                      RestaurantUserProfileRepository userProfileRepository,
                                      NotificationRepository notificationRepository,
                                      RecipeVersionRepository recipeVersionRepository) {
        this.supplyRepository = supplyRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationRepository = notificationRepository;
        this.recipeVersionRepository = recipeVersionRepository;
    }

    /**
     * Registra un ajuste manual (aumento o disminución) en las existencias de un insumo.
     *
     * @param supplyId       Identificador del insumo (de la ruta URL o cuerpo)
     * @param request        Datos del ajuste manual (tipo, cantidad, motivo)
     * @param authentication Información de autenticación del usuario administrador
     * @return Confirmación del ajuste con detalle de existencias, kardex y platillos afectados
     */
    @Transactional
    public InventoryAdjustmentResponse registerAdjustment(Long supplyId,
                                                          CreateInventoryAdjustmentRequest request,
                                                          Authentication authentication) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        Long targetSupplyId = supplyId != null ? supplyId : (request != null ? request.supplyId() : null);
        if (targetSupplyId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_supply_id",
                    "Insumo no especificado",
                    "Debe seleccionar o indicar el insumo para registrar el ajuste"
            );
        }

        if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_adjustment_reason",
                    "Motivo obligatorio",
                    "El motivo del ajuste es obligatorio"
            );
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_adjustment_quantity",
                    "Cantidad no válida",
                    "La cantidad debe ser mayor que cero"
            );
        }

        String rawType = request.adjustmentType() != null ? request.adjustmentType().trim().toUpperCase() : "";
        boolean isIncrease;
        String kardexType;
        if (rawType.equals("AUMENTO") || rawType.equals("INCREMENTO") || rawType.equals("ENTRADA") || rawType.equals("AJUSTE_ENTRADA") || rawType.equals("+")) {
            isIncrease = true;
            kardexType = "AJUSTE_ENTRADA";
        } else if (rawType.equals("DISMINUCION") || rawType.equals("DISMINUCIÓN") || rawType.equals("DECREMENTO") || rawType.equals("SALIDA") || rawType.equals("AJUSTE_SALIDA") || rawType.equals("-")) {
            isIncrease = false;
            kardexType = "AJUSTE_SALIDA";
        } else {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_adjustment_type",
                    "Tipo de ajuste no válido",
                    "Debe indicar si el ajuste es de aumento o disminución"
            );
        }

        Supply supply = supplyRepository.findByIdAndRestaurantId(targetSupplyId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "supply_not_found",
                        "Insumo no encontrado",
                        "No se encontró un insumo con el identificador " + targetSupplyId
                ));

        if (!supply.isActive()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "inactive_supply",
                    "Insumo inactivo",
                    "No se pueden realizar ajustes en un insumo inactivo"
            );
        }

        BigDecimal currentStock = supply.getCurrentStock() != null ? supply.getCurrentStock() : BigDecimal.ZERO;

        if (!isIncrease && request.quantity().compareTo(currentStock) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "quantity_exceeds_stock",
                    "Cantidad excede existencias",
                    "La cantidad excede las existencias disponibles (disponible: " + currentStock + ")"
            );
        }

        BigDecimal newStock = isIncrease
                ? currentStock.add(request.quantity())
                : currentStock.subtract(request.quantity());

        Long responsibleUserId = resolveAdminUserId(authentication);

        InventoryMovement movement = new InventoryMovement(
                restaurantId,
                supply,
                kardexType,
                request.quantity(),
                null,
                request.reason().trim(),
                responsibleUserId
        );
        movement.setPreviousStock(currentStock);
        movement.setResultingStock(newStock);
        movement.setUnitCostSnapshot(supply.getCurrentUnitCost() != null ? supply.getCurrentUnitCost() : BigDecimal.ZERO);
        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        supply.setCurrentStock(newStock);
        supplyRepository.save(supply);

        if (entityManager != null) {
            entityManager.flush();
            entityManager.refresh(supply);
        }

        boolean alertGenerated = syncLowStockNotification(restaurantId, supply, newStock);

        List<AffectedDishResponse> affectedDishes = evaluateAffectedDishes(restaurantId, supply.getId());

        String responsibleUserName = resolveUserName(responsibleUserId);

        String unit = (supply.getMeasurementUnit() != null)
                ? supply.getMeasurementUnit().getAbbreviation()
                : "u";

        String confirmMsg = isIncrease
                ? "Ajuste manual de aumento registrado correctamente"
                : "Ajuste manual de disminución registrado correctamente";

        return new InventoryAdjustmentResponse(
                confirmMsg,
                savedMovement.getId(),
                supply.getId(),
                supply.getCode(),
                supply.getName(),
                isIncrease ? "AUMENTO" : "DISMINUCION",
                kardexType,
                request.quantity(),
                currentStock,
                newStock,
                unit,
                request.reason().trim(),
                responsibleUserId,
                responsibleUserName,
                savedMovement.getCreatedAt(),
                alertGenerated,
                affectedDishes
        );
    }

    private boolean syncLowStockNotification(Long restaurantId, Supply supply, BigDecimal newStock) {
        if (supply.getMinimumStock() != null && newStock.compareTo(supply.getMinimumStock()) <= 0) {
            boolean exists = notificationRepository
                    .existsByRestaurantIdAndTypeAndEntityAndEntityIdAndReadFalse(
                            restaurantId, "STOCK_BAJO", "INSUMO", supply.getId().toString());

            if (!exists) {
                boolean isOutOfStock = newStock.compareTo(BigDecimal.ZERO) <= 0;
                String message = isOutOfStock
                        ? "El insumo " + supply.getName() + " se encuentra agotado"
                        : "El insumo " + supply.getName() + " alcanzo el nivel minimo de stock";

                Notification notification = new Notification(
                        restaurantId,
                        null,
                        1L,
                        "STOCK_BAJO",
                        "Insumo con stock bajo",
                        message,
                        "INSUMO",
                        supply.getId().toString(),
                        isOutOfStock ? "CRITICA" : "ALTA");

                notificationRepository.save(notification);
                return true;
            }
        } else if (supply.getMinimumStock() != null && newStock.compareTo(supply.getMinimumStock()) > 0) {
            notificationRepository.markAsReadByEntity(
                    restaurantId, "STOCK_BAJO", "INSUMO", supply.getId().toString(), Instant.now());
        }
        return false;
    }

    private List<AffectedDishResponse> evaluateAffectedDishes(Long restaurantId, Long supplyId) {
        List<AffectedDishResponse> list = new ArrayList<>();
        List<RecipeVersion> activeRecipes = recipeVersionRepository.findActiveWithDetailsByRestaurantId(restaurantId);

        for (RecipeVersion rv : activeRecipes) {
            boolean containsSupply = rv.getDetails() != null && rv.getDetails().stream()
                    .anyMatch(d -> d.getSupply() != null && d.getSupply().getId().equals(supplyId));

            if (containsSupply) {
                Dish dish = rv.getDish();
                int portions = calculatePortionsFromRecipe(rv);
                boolean available;
                String reason = null;

                if (!dish.isActive()) {
                    available = false;
                    reason = "INACTIVO";
                } else if (!dish.isManualAvailable()) {
                    available = false;
                    reason = "MANUAL";
                } else if (portions < 1) {
                    available = false;
                    reason = "FALTA_INSUMOS";
                } else {
                    available = true;
                }

                list.add(new AffectedDishResponse(
                        dish.getId(),
                        dish.getCode(),
                        dish.getName(),
                        available,
                        portions,
                        reason
                ));
            }
        }

        return list;
    }

    private int calculatePortionsFromRecipe(RecipeVersion recipeVersion) {
        if (recipeVersion == null || recipeVersion.getDetails() == null || recipeVersion.getDetails().isEmpty()) {
            return 0;
        }

        int minPortions = Integer.MAX_VALUE;

        for (RecipeDetail detail : recipeVersion.getDetails()) {
            Supply s = detail.getSupply();
            if (s == null || !s.isActive()) {
                return 0;
            }

            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = s.getMeasurementUnit();

            BigDecimal quantity = detail.getQuantity();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal urBaseFactor = recipeUnit != null && recipeUnit.getBaseFactor() != null
                    ? recipeUnit.getBaseFactor()
                    : BigDecimal.ONE;
            BigDecimal usBaseFactor = stockUnit != null && stockUnit.getBaseFactor() != null
                    ? stockUnit.getBaseFactor()
                    : BigDecimal.ONE;

            if (usBaseFactor.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                    .divide(usBaseFactor, 6, RoundingMode.HALF_UP);

            if (proportionalQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal currentStock = s.getCurrentStock() != null
                    ? s.getCurrentStock()
                    : BigDecimal.ZERO;

            if (currentStock.compareTo(proportionalQty) < 0) {
                return 0;
            }

            int portions = currentStock.divide(proportionalQty, 0, RoundingMode.FLOOR).intValue();
            if (portions < minPortions) {
                minPortions = portions;
            }
        }

        return minPortions == Integer.MAX_VALUE ? 0 : minPortions;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileRepository.findById(userId)
                .map(p -> (p.getFirstName() + " " + p.getLastName()).trim())
                .orElse("Usuario #" + userId);
    }

    private Long resolveAdminUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.userId();
        }
        return 1L;
    }
}
