package com.restaurante.application.combo;

import com.restaurante.domain.model.Combo;
import com.restaurante.domain.model.ComboDetail;
import com.restaurante.domain.model.Dish;
import com.restaurante.domain.repository.ComboDetailRepository;
import com.restaurante.domain.repository.ComboRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.combo.ComboDetailResponse;
import com.restaurante.web.dto.combo.ComboItemRequest;
import com.restaurante.web.dto.combo.ComboRegistrationResponse;
import com.restaurante.web.dto.combo.ComboResponse;
import com.restaurante.web.dto.combo.ComboRetirementResponse;
import com.restaurante.web.dto.combo.ComboUpdateResponse;
import com.restaurante.web.dto.combo.CreateComboRequest;
import com.restaurante.web.dto.combo.UpdateComboRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para la gestión de combos y promociones comerciales de platillos.
 */
@Service
public class ComboService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ComboRepository comboRepository;
    private final ComboDetailRepository comboDetailRepository;
    private final DishRepository dishRepository;

    public ComboService(ComboRepository comboRepository,
                        ComboDetailRepository comboDetailRepository,
                        DishRepository dishRepository) {
        this.comboRepository = comboRepository;
        this.comboDetailRepository = comboDetailRepository;
        this.dishRepository = dishRepository;
    }

    /**
     * Registra un nuevo combo o promoción comercial agrupando dos o más platillos con un precio especial.
     *
     * @param request Datos de creación del combo
     * @return Confirmación y detalle consolidado del combo registrado
     */
    @Transactional
    public ComboRegistrationResponse createCombo(CreateComboRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_request_body",
                    "Solicitud vacía",
                    "Los datos del combo son obligatorios"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_combo_name",
                    "Nombre obligatorio",
                    "El nombre del combo es obligatorio"
            );
        }

        String trimmedName = request.name().trim();
        if (comboRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, trimmedName)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "duplicate_combo_name",
                    "Nombre duplicado",
                    "Ya existe un combo registrado con el nombre '" + trimmedName + "'"
            );
        }

        if (request.salePrice() == null || request.salePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_combo_price",
                    "Precio inválido",
                    "El precio debe ser mayor que cero"
            );
        }

        if (request.items() == null || request.items().size() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_combo_dishes",
                    "Platillos insuficientes",
                    "El combo debe incluir al menos dos platillos"
            );
        }

        Map<Long, Short> dishQuantities = new LinkedHashMap<>();
        for (ComboItemRequest item : request.items()) {
            if (item.dishId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_dish_id",
                        "Platillo no especificado",
                        "Debe indicar el identificador de cada platillo del combo"
                );
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_dish_quantity",
                        "Cantidad inválida",
                        "La cantidad debe ser mayor que cero"
                );
            }
            dishQuantities.merge(item.dishId(), item.quantity(), (existing, added) -> (short) (existing + added));
        }

        if (dishQuantities.size() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_combo_dishes",
                    "Platillos insuficientes",
                    "El combo debe incluir al menos dos platillos"
            );
        }

        List<Dish> validatedDishes = new ArrayList<>();
        short calculatedMaxPreparationTime = 15;

        for (Map.Entry<Long, Short> entry : dishQuantities.entrySet()) {
            Long dishId = entry.getKey();
            Dish dish = dishRepository.findByIdAndRestaurantId(dishId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "dish_not_found",
                            "Platillo no encontrado",
                            "No se encontró un platillo con el identificador " + dishId
                    ));

            if (!dish.isActive()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "inactive_dish_not_allowed",
                        "Platillo retirado",
                        "El platillo '" + dish.getName() + "' se encuentra retirado del menú. Solamente pueden agregarse platillos activos"
                );
            }

            if (dish.getPreparationTimeMinutes() != null && dish.getPreparationTimeMinutes() > calculatedMaxPreparationTime) {
                calculatedMaxPreparationTime = dish.getPreparationTimeMinutes();
            }

            validatedDishes.add(dish);
        }

        if (request.startDate() != null && request.endDate() != null && !request.endDate().isAfter(request.startDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_date_range",
                    "Rango de fechas inválido",
                    "La fecha de fin debe ser posterior a la fecha de inicio"
            );
        }

        String code = resolveComboCode(restaurantId, request.code());
        Short preparationTime = (request.preparationTimeMinutes() != null && request.preparationTimeMinutes() > 0)
                ? request.preparationTimeMinutes()
                : calculatedMaxPreparationTime;

        Combo combo = new Combo(
                restaurantId,
                code,
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                request.salePrice(),
                request.imageUrl() != null ? request.imageUrl().trim() : null,
                preparationTime,
                request.startDate(),
                request.endDate()
        );
        Combo savedCombo = comboRepository.save(combo);

        short visualOrder = 0;
        for (Dish dish : validatedDishes) {
            short quantity = dishQuantities.get(dish.getId());
            ComboDetail detail = new ComboDetail(savedCombo, dish, quantity, visualOrder++);
            comboDetailRepository.save(detail);
        }

        return new ComboRegistrationResponse(
                "Combo registrado exitosamente",
                mapToResponse(savedCombo)
        );
    }

    /**
     * Consulta el detalle de un combo por su identificador.
     *
     * @param id Identificador único del combo
     * @return Detalle comercial y platillos del combo
     */
    @Transactional(readOnly = true)
    public ComboResponse getComboById(Long id) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_combo_id",
                    "Combo no especificado",
                    "Debe indicar el identificador del combo"
            );
        }

        Combo combo = comboRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "combo_not_found",
                        "Combo no encontrado",
                        "No se encontró un combo con el identificador " + id
                ));

        return mapToResponse(combo);
    }

    /**
     * Consulta el catálogo de combos y promociones registrados.
     *
     * @param search Término opcional de búsqueda por nombre o código
     * @param active Filtro opcional por estado activo
     * @return Listado consolidado de combos
     */
    @Transactional(readOnly = true)
    public List<ComboResponse> listCombos(String search, Boolean active) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        return comboRepository.findByRestaurantIdAndFilters(restaurantId, active, pattern)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Actualiza la información de un combo o promoción existente, incluyendo platillos, cantidades y precio especial.
     *
     * @param id Identificador único del combo a actualizar
     * @param request Datos actualizados del combo
     * @return Confirmación y detalle actualizado del combo
     */
    @Transactional
    public ComboUpdateResponse updateCombo(Long id, UpdateComboRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_combo_id",
                    "Combo no especificado",
                    "Debe indicar el identificador del combo"
            );
        }

        Combo combo = comboRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "combo_not_found",
                        "Combo no encontrado",
                        "No se encontró un combo con el identificador " + id
                ));

        if (!combo.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "combo_retired",
                    "Combo retirado",
                    "El combo se encuentra retirado del menú y no se puede modificar"
            );
        }

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_request_body",
                    "Solicitud vacía",
                    "Los datos del combo son obligatorios"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_combo_name",
                    "Nombre obligatorio",
                    "El nombre del combo es obligatorio"
            );
        }

        String trimmedName = request.name().trim();
        if (comboRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, trimmedName, id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "duplicate_combo_name",
                    "Nombre duplicado",
                    "Ya existe otro combo registrado con el nombre '" + trimmedName + "'"
            );
        }

        if (request.salePrice() == null || request.salePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_combo_price",
                    "Precio inválido",
                    "El precio especial debe ser mayor a cero"
            );
        }

        if (request.items() == null || request.items().size() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_combo_dishes",
                    "Platillos insuficientes",
                    "Un combo debe contener al menos dos platillos"
            );
        }

        Map<Long, Short> dishQuantities = new LinkedHashMap<>();
        for (ComboItemRequest item : request.items()) {
            if (item.dishId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_dish_id",
                        "Platillo no especificado",
                        "Debe indicar el identificador de cada platillo del combo"
                );
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_dish_quantity",
                        "Cantidad inválida",
                        "La cantidad debe ser mayor que cero"
                );
            }
            dishQuantities.merge(item.dishId(), item.quantity(), (existing, added) -> (short) (existing + added));
        }

        if (dishQuantities.size() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_combo_dishes",
                    "Platillos insuficientes",
                    "Un combo debe contener al menos dos platillos"
            );
        }

        List<Dish> validatedDishes = new ArrayList<>();
        short calculatedMaxPreparationTime = 15;

        for (Map.Entry<Long, Short> entry : dishQuantities.entrySet()) {
            Long dishId = entry.getKey();
            Dish dish = dishRepository.findByIdAndRestaurantId(dishId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "dish_not_found",
                            "Platillo no encontrado",
                            "No se encontró un platillo con el identificador " + dishId
                    ));

            if (!dish.isActive()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "inactive_dish_not_allowed",
                        "Platillo retirado",
                        "El platillo '" + dish.getName() + "' se encuentra retirado del menú. Solamente pueden agregarse platillos activos"
                );
            }

            if (dish.getPreparationTimeMinutes() != null && dish.getPreparationTimeMinutes() > calculatedMaxPreparationTime) {
                calculatedMaxPreparationTime = dish.getPreparationTimeMinutes();
            }

            validatedDishes.add(dish);
        }

        if (request.startDate() != null && request.endDate() != null && !request.endDate().isAfter(request.startDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_date_range",
                    "Rango de fechas inválido",
                    "La fecha de fin debe ser posterior a la fecha de inicio"
            );
        }

        if (request.code() != null && !request.code().isBlank()) {
            String sanitizedCode = request.code().trim().toUpperCase();
            if (comboRepository.existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(restaurantId, sanitizedCode, id)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "duplicate_combo_code",
                        "Código duplicado",
                        "Ya existe otro combo registrado con el código '" + sanitizedCode + "'"
                );
            }
            combo.setCode(sanitizedCode);
        }

        combo.setName(trimmedName);
        combo.setDescription(request.description() != null ? request.description().trim() : null);
        combo.setSalePrice(request.salePrice());
        combo.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);
        combo.setStartDate(request.startDate());
        combo.setEndDate(request.endDate());

        if (request.manualAvailable() != null) {
            combo.setManualAvailable(request.manualAvailable());
        }

        Short preparationTime = (request.preparationTimeMinutes() != null && request.preparationTimeMinutes() > 0)
                ? request.preparationTimeMinutes()
                : calculatedMaxPreparationTime;
        combo.setPreparationTimeMinutes(preparationTime);

        comboDetailRepository.deleteByComboId(combo.getId());
        comboDetailRepository.flush();

        short visualOrder = 0;
        for (Dish dish : validatedDishes) {
            short quantity = dishQuantities.get(dish.getId());
            ComboDetail detail = new ComboDetail(combo, dish, quantity, visualOrder++);
            comboDetailRepository.save(detail);
        }

        Combo savedCombo = comboRepository.save(combo);

        return new ComboUpdateResponse("Combo actualizado exitosamente", mapToResponse(savedCombo));
    }

    /**
     * Retira un combo o promoción del menú marcándolo como inactivo sin eliminar su información histórica.
     *
     * @param id Identificador único del combo a retirar
     * @return Confirmación y detalle del combo retirado
     */
    @Transactional
    public ComboRetirementResponse retireCombo(Long id) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_combo_id",
                    "Combo no especificado",
                    "Debe indicar el identificador del combo a retirar"
            );
        }

        Combo combo = comboRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "combo_not_found",
                        "Combo no encontrado",
                        "No se encontró un combo con el identificador " + id
                ));

        if (!combo.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "combo_already_retired",
                    "Combo ya retirado",
                    "El combo ya se encuentra retirado"
            );
        }

        combo.setActive(false);
        combo.setManualAvailable(false);
        Combo savedCombo = comboRepository.save(combo);

        return new ComboRetirementResponse(
                "Combo retirado exitosamente",
                mapToResponse(savedCombo)
        );
    }

    private String resolveComboCode(Long restaurantId, String providedCode) {
        if (providedCode != null && !providedCode.isBlank()) {
            String sanitized = providedCode.trim().toUpperCase();
            if (comboRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, sanitized)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "duplicate_combo_code",
                        "Código duplicado",
                        "Ya existe un combo registrado con el código '" + sanitized + "'"
                );
            }
            return sanitized;
        }

        long nextIndex = comboRepository.countByRestaurantId(restaurantId) + 1;
        String generated = String.format("COM-%04d", nextIndex);
        while (comboRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, generated)) {
            nextIndex++;
            generated = String.format("COM-%04d", nextIndex);
        }
        return generated;
    }

    private ComboResponse mapToResponse(Combo combo) {
        List<ComboDetail> details = comboDetailRepository.findByComboIdWithDish(combo.getId());

        BigDecimal regularPriceSum = BigDecimal.ZERO;
        List<ComboDetailResponse> items = new ArrayList<>();

        for (ComboDetail detail : details) {
            Dish dish = detail.getDish();
            BigDecimal dishRegularPrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
            BigDecimal subtotal = dishRegularPrice.multiply(BigDecimal.valueOf(detail.getQuantity()));
            regularPriceSum = regularPriceSum.add(subtotal);

            items.add(new ComboDetailResponse(
                    detail.getId(),
                    dish.getId(),
                    dish.getCode(),
                    dish.getName(),
                    dishRegularPrice,
                    detail.getQuantity(),
                    detail.getVisualOrder()
            ));
        }

        BigDecimal estimatedSavings = BigDecimal.ZERO;
        if (regularPriceSum.compareTo(combo.getSalePrice()) > 0) {
            estimatedSavings = regularPriceSum.subtract(combo.getSalePrice());
        }

        return new ComboResponse(
                combo.getId(),
                combo.getCode(),
                combo.getName(),
                combo.getDescription(),
                combo.getSalePrice(),
                regularPriceSum,
                estimatedSavings,
                combo.getImageUrl(),
                combo.getPreparationTimeMinutes(),
                combo.isManualAvailable(),
                combo.getStartDate(),
                combo.getEndDate(),
                combo.isActive(),
                items,
                combo.getCreatedAt(),
                combo.getUpdatedAt()
        );
    }
}
