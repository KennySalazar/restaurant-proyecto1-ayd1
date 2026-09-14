package com.restaurante.application.dish;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.DishCategory;
import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.RecipeDetail;
import com.restaurante.domain.model.RecipeVersion;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.DishCategoryRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.dish.CreateDishRequest;
import com.restaurante.web.dto.dish.DishCategoryResponse;
import com.restaurante.web.dto.dish.DishRegistrationResponse;
import com.restaurante.web.dto.dish.DishResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la gestión y consulta del catálogo de platillos y categorías del menú.
 */
@Service
public class DishService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final DishRepository dishRepository;
    private final DishCategoryRepository dishCategoryRepository;
    private final RecipeVersionRepository recipeVersionRepository;

    public DishService(DishRepository dishRepository,
                       DishCategoryRepository dishCategoryRepository,
                       RecipeVersionRepository recipeVersionRepository) {
        this.dishRepository = dishRepository;
        this.dishCategoryRepository = dishCategoryRepository;
        this.recipeVersionRepository = recipeVersionRepository;
    }

    /**
     * Registra un nuevo platillo en el catálogo con su información comercial y tiempo estimado de preparación.
     *
     * @param request Datos del platillo a registrar
     * @return Confirmación y detalle del platillo registrado
     */
    @Transactional
    public DishRegistrationResponse registerDish(CreateDishRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_name",
                    "Nombre obligatorio",
                    "El nombre del platillo es obligatorio"
            );
        }

        String trimmedName = request.name().trim();
        if (dishRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, trimmedName)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "duplicate_dish_name",
                    "Nombre duplicado",
                    "Ya existe un platillo registrado con el nombre '" + trimmedName + "'"
            );
        }

        if (request.categoryId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_category_id",
                    "Categoría obligatoria",
                    "Debe seleccionar una categoría para el platillo"
            );
        }

        DishCategory category = dishCategoryRepository.findByIdAndRestaurantId(request.categoryId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "category_not_found",
                        "Categoría no válida",
                        "La categoría seleccionada no existe"
                ));

        if (request.salePrice() == null || request.salePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_sale_price",
                    "Precio inválido",
                    "El precio de venta debe ser mayor que cero"
            );
        }

        if (request.preparationTimeMinutes() == null || request.preparationTimeMinutes() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_preparation_time",
                    "Tiempo de preparación inválido",
                    "El tiempo estimado de preparación debe ser mayor que cero"
            );
        }

        String code = resolveDishCode(restaurantId, request.code());

        Dish dish = new Dish(
                restaurantId,
                category,
                code,
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                request.salePrice(),
                request.imageUrl() != null ? request.imageUrl().trim() : null,
                request.preparationTimeMinutes()
        );

        Dish saved = dishRepository.save(dish);

        return new DishRegistrationResponse(
                "Platillo registrado exitosamente",
                mapToResponse(saved)
        );
    }

    /**
     * Consulta el catálogo de platillos registrados con su información comercial, tiempo de preparación y disponibilidad actual.
     * Permite filtrar opcionalmente por categoría o término de búsqueda.
     *
     * @param categoryId Identificador opcional de categoría para filtrar
     * @param search     Término opcional de búsqueda por nombre o código de platillo
     * @return Listado de platillos con su disponibilidad y motivos de indisponibilidad evaluados
     */
    @Transactional(readOnly = true)
    public List<DishResponse> listDishes(Long categoryId, String search) {
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        List<Dish> dishes;
        if (categoryId != null && pattern != null) {
            dishes = dishRepository.findByRestaurantIdAndActiveTrueAndCategoryIdAndSearchPattern(
                    DEFAULT_RESTAURANT_ID, categoryId, pattern);
        } else if (categoryId != null) {
            dishes = dishRepository.findByRestaurantIdAndActiveTrueAndCategoryIdOrderByNameAsc(
                    DEFAULT_RESTAURANT_ID, categoryId);
        } else if (pattern != null) {
            dishes = dishRepository.findByRestaurantIdAndActiveTrueAndSearchPattern(
                    DEFAULT_RESTAURANT_ID, pattern);
        } else {
            dishes = dishRepository.findByRestaurantIdAndActiveTrueOrderByNameAsc(
                    DEFAULT_RESTAURANT_ID);
        }

        if (dishes.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecipeVersion> activeVersions = recipeVersionRepository.findActiveWithDetailsByRestaurantId(DEFAULT_RESTAURANT_ID);
        Map<Long, RecipeVersion> recipeByDishId = activeVersions.stream()
                .collect(Collectors.toMap(rv -> rv.getDish().getId(), rv -> rv, (first, second) -> first));

        return dishes.stream()
                .map(dish -> mapToResponseWithRecipe(dish, recipeByDishId.get(dish.getId())))
                .toList();
    }

    /**
     * Consulta todos los platillos activos del catálogo.
     *
     * @return Listado completo de platillos
     */
    @Transactional(readOnly = true)
    public List<DishResponse> listDishes() {
        return listDishes(null, null);
    }

    /**
     * Consulta la información y disponibilidad de un platillo específico por su identificador.
     *
     * @param id Identificador único del platillo
     * @return Información detallada del platillo
     */
    @Transactional(readOnly = true)
    public DishResponse getDishById(Long id) {
        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo"
            );
        }

        Dish dish = dishRepository.findByIdAndRestaurantId(id, DEFAULT_RESTAURANT_ID)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + id
                ));

        RecipeVersion activeRecipe = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                .orElse(null);

        return mapToResponseWithRecipe(dish, activeRecipe);
    }

    /**
     * Consulta las categorías de platillos disponibles en el restaurante para selección en el menú.
     *
     * @return Listado de categorías ordenadas para su presentación visual
     */
    @Transactional(readOnly = true)
    public List<DishCategoryResponse> listDishCategories() {
        return dishCategoryRepository.findByRestaurantIdOrderByVisualOrderAsc(DEFAULT_RESTAURANT_ID)
                .stream()
                .map(this::mapCategoryToResponse)
                .toList();
    }

    private String resolveDishCode(Long restaurantId, String providedCode) {
        if (providedCode != null && !providedCode.isBlank()) {
            String sanitized = providedCode.trim().toUpperCase();
            if (dishRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, sanitized)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "duplicate_dish_code",
                        "Código duplicado",
                        "Ya existe un platillo registrado con el código '" + sanitized + "'"
                );
            }
            return sanitized;
        }

        long nextIndex = dishRepository.countByRestaurantId(restaurantId) + 1;
        String generated = String.format("PLA-%04d", nextIndex);
        while (dishRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, generated)) {
            nextIndex++;
            generated = String.format("PLA-%04d", nextIndex);
        }
        return generated;
    }

    private DishResponse mapToResponse(Dish dish) {
        RecipeVersion activeRecipe = recipeVersionRepository.findActiveWithDetailsByDishId(dish.getId())
                .orElse(null);
        return mapToResponseWithRecipe(dish, activeRecipe);
    }

    private DishResponse mapToResponseWithRecipe(Dish dish, RecipeVersion activeRecipe) {
        boolean active = dish.isActive();
        boolean manualAvailable = dish.isManualAvailable();

        boolean available;
        int availablePortions = 0;
        String unavailabilityReason = null;
        String unavailabilityReasonDescription = null;

        if (!active) {
            available = false;
            availablePortions = 0;
            unavailabilityReason = "INACTIVO";
            unavailabilityReasonDescription = "Platillo inactivo";
        } else if (!manualAvailable) {
            available = false;
            unavailabilityReason = "MANUAL";
            unavailabilityReasonDescription = "Indisponibilidad manual";
            if (activeRecipe != null && activeRecipe.getDetails() != null && !activeRecipe.getDetails().isEmpty()) {
                availablePortions = calculatePortionsFromRecipe(activeRecipe);
            } else {
                availablePortions = 0;
            }
        } else {
            // Platillo activo y con disponibilidad manual habilitada
            if (activeRecipe == null || activeRecipe.getDetails() == null || activeRecipe.getDetails().isEmpty()) {
                available = false;
                availablePortions = 0;
                unavailabilityReason = "FALTA_INSUMOS";
                unavailabilityReasonDescription = "Falta de insumos";
            } else {
                availablePortions = calculatePortionsFromRecipe(activeRecipe);
                if (availablePortions < 1) {
                    available = false;
                    unavailabilityReason = "FALTA_INSUMOS";
                    unavailabilityReasonDescription = "Falta de insumos";
                } else {
                    available = true;
                    unavailabilityReason = null;
                    unavailabilityReasonDescription = null;
                }
            }
        }

        return new DishResponse(
                dish.getId(),
                dish.getCode(),
                dish.getName(),
                dish.getDescription(),
                dish.getCategory().getId(),
                dish.getCategory().getName(),
                dish.getSalePrice(),
                dish.getImageUrl(),
                dish.getPreparationTimeMinutes(),
                available,
                availablePortions,
                unavailabilityReason,
                unavailabilityReasonDescription,
                dish.isManualAvailable(),
                dish.isActive(),
                dish.getCreatedAt(),
                dish.getUpdatedAt()
        );
    }

    private int calculatePortionsFromRecipe(RecipeVersion recipeVersion) {
        if (recipeVersion == null || recipeVersion.getDetails() == null || recipeVersion.getDetails().isEmpty()) {
            return 0;
        }

        int minPortions = Integer.MAX_VALUE;

        for (RecipeDetail detail : recipeVersion.getDetails()) {
            Supply supply = detail.getSupply();
            if (supply == null || !supply.isActive()) {
                return 0;
            }

            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = supply.getMeasurementUnit();

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

            BigDecimal currentStock = supply.getCurrentStock() != null
                    ? supply.getCurrentStock()
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

    private DishCategoryResponse mapCategoryToResponse(DishCategory category) {
        return new DishCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getVisualOrder()
        );
    }
}
