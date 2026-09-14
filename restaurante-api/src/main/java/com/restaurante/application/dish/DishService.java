package com.restaurante.application.dish;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.DishCategory;
import com.restaurante.domain.repository.DishCategoryRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.dish.CreateDishRequest;
import com.restaurante.web.dto.dish.DishCategoryResponse;
import com.restaurante.web.dto.dish.DishRegistrationResponse;
import com.restaurante.web.dto.dish.DishResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de aplicación para la gestión de platillos y categorías del menú.
 */
@Service
public class DishService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final DishRepository dishRepository;
    private final DishCategoryRepository dishCategoryRepository;

    public DishService(DishRepository dishRepository, DishCategoryRepository dishCategoryRepository) {
        this.dishRepository = dishRepository;
        this.dishCategoryRepository = dishCategoryRepository;
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
                dish.isManualAvailable(),
                dish.isActive(),
                dish.getCreatedAt(),
                dish.getUpdatedAt()
        );
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
