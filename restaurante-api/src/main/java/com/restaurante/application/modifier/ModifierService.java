package com.restaurante.application.modifier;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.DishModifier;
import com.restaurante.domain.model.Modifier;
import com.restaurante.domain.repository.DishModifierRepository;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.ModifierRecipeVersionRepository;
import com.restaurante.domain.repository.ModifierRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.modifier.AssociateDishesRequest;
import com.restaurante.web.dto.modifier.CreateModifierRequest;
import com.restaurante.web.dto.modifier.ModifierDeactivationResponse;
import com.restaurante.web.dto.modifier.ModifierDishItemResponse;
import com.restaurante.web.dto.modifier.ModifierRegistrationResponse;
import com.restaurante.web.dto.modifier.ModifierResponse;
import com.restaurante.web.dto.modifier.ModifierUpdateResponse;
import com.restaurante.web.dto.modifier.UpdateModifierRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de negocio para la gestión de modificadores de platillos.
 */
@Service
public class ModifierService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final ModifierRepository modifierRepository;
    private final DishModifierRepository dishModifierRepository;
    private final DishRepository dishRepository;
    private final ModifierRecipeVersionRepository modifierRecipeVersionRepository;

    public ModifierService(ModifierRepository modifierRepository,
                           DishModifierRepository dishModifierRepository,
                           DishRepository dishRepository,
                           ModifierRecipeVersionRepository modifierRecipeVersionRepository) {
        this.modifierRepository = modifierRepository;
        this.dishModifierRepository = dishModifierRepository;
        this.dishRepository = dishRepository;
        this.modifierRecipeVersionRepository = modifierRecipeVersionRepository;
    }

    /**
     * Registra un nuevo modificador y lo asocia con uno o varios platillos.
     *
     * @param request Datos de registro del modificador
     * @return Confirmación y detalle del modificador creado
     */
    @Transactional
    public ModifierRegistrationResponse registerModifier(CreateModifierRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_request_body",
                    "Solicitud vacía",
                    "Los datos del modificador son obligatorios"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_name",
                    "Nombre obligatorio",
                    "El nombre del modificador es obligatorio"
            );
        }

        String trimmedName = request.name().trim();
        if (modifierRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, trimmedName)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "duplicate_modifier_name",
                    "Nombre duplicado",
                    "Ya existe un modificador registrado con el nombre '" + trimmedName + "'"
            );
        }

        BigDecimal additionalPrice = BigDecimal.ZERO;
        if (request.additionalPrice() != null) {
            if (request.additionalPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_additional_price",
                        "Costo adicional inválido",
                        "El costo adicional no puede ser negativo"
                );
            }
            additionalPrice = request.additionalPrice();
        }

        if (request.dishIds() == null || request.dishIds().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_associated_dishes",
                    "Platillos no especificados",
                    "Debe asociar el modificador al menos con un platillo"
            );
        }

        Set<Long> uniqueDishIds = new LinkedHashSet<>(request.dishIds());
        List<Dish> associatedDishes = validateAndGetDishes(restaurantId, uniqueDishIds);

        String code = resolveModifierCode(restaurantId, request.code());

        Modifier modifier = new Modifier(
                restaurantId,
                code,
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                additionalPrice
        );
        Modifier savedModifier = modifierRepository.save(modifier);

        short visualOrder = 0;
        for (Dish dish : associatedDishes) {
            DishModifier dishModifier = new DishModifier(dish, savedModifier, false, (short) 1, visualOrder++);
            dishModifierRepository.save(dishModifier);
        }

        return new ModifierRegistrationResponse(
                "Modificador registrado exitosamente",
                mapToResponse(savedModifier)
        );
    }

    /**
     * Actualiza la información comercial y los platillos asociados de un modificador.
     *
     * @param id      Identificador único del modificador
     * @param request Datos actualizados
     * @return Confirmación y detalle actualizado
     */
    @Transactional
    public ModifierUpdateResponse updateModifier(Long id, UpdateModifierRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador a modificar"
            );
        }

        Modifier modifier = modifierRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + id
                ));

        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_name",
                    "Nombre obligatorio",
                    "El nombre del modificador es obligatorio"
            );
        }

        String trimmedName = request.name().trim();
        if (modifierRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, trimmedName, id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "duplicate_modifier_name",
                    "Nombre duplicado",
                    "Ya existe otro modificador registrado con el nombre '" + trimmedName + "'"
            );
        }

        if (request.additionalPrice() != null) {
            if (request.additionalPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_additional_price",
                        "Costo adicional inválido",
                        "El costo adicional no puede ser negativo"
                );
            }
            modifier.setAdditionalPrice(request.additionalPrice());
        }

        if (request.code() != null && !request.code().isBlank()) {
            String sanitizedCode = request.code().trim().toUpperCase();
            if (modifierRepository.existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(restaurantId, sanitizedCode, id)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "duplicate_modifier_code",
                        "Código duplicado",
                        "Ya existe otro modificador registrado con el código '" + sanitizedCode + "'"
                );
            }
            modifier.setCode(sanitizedCode);
        }

        modifier.setName(trimmedName);
        modifier.setDescription(request.description() != null ? request.description().trim() : null);

        if (request.dishIds() != null) {
            if (request.dishIds().isEmpty()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_associated_dishes",
                        "Platillos no especificados",
                        "Debe asociar el modificador al menos con un platillo"
                );
            }

            Set<Long> uniqueDishIds = new LinkedHashSet<>(request.dishIds());
            List<Dish> associatedDishes = validateAndGetDishes(restaurantId, uniqueDishIds);

            dishModifierRepository.deleteByModifierId(modifier.getId());
            dishModifierRepository.flush();

            short visualOrder = 0;
            for (Dish dish : associatedDishes) {
                DishModifier dishModifier = new DishModifier(dish, modifier, false, (short) 1, visualOrder++);
                dishModifierRepository.save(dishModifier);
            }
        }

        Modifier saved = modifierRepository.save(modifier);

        return new ModifierUpdateResponse(
                "Modificador actualizado exitosamente",
                mapToResponse(saved)
        );
    }

    /**
     * Asocia un modificador activo a múltiples platillos.
     *
     * @param id      Identificador único del modificador
     * @param request Identificadores de platillos a asociar
     * @return Confirmación y detalle actualizado
     */
    @Transactional
    public ModifierUpdateResponse associateDishes(Long id, AssociateDishesRequest request) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador"
            );
        }

        Modifier modifier = modifierRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + id
                ));

        if (!modifier.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "modifier_inactive",
                    "Modificador inactivo",
                    "No se pueden asociar platillos a un modificador inactivo"
            );
        }

        if (request == null || request.dishIds() == null || request.dishIds().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_associated_dishes",
                    "Platillos no especificados",
                    "Debe especificar al menos un platillo para asociar"
            );
        }

        Set<Long> uniqueDishIds = new LinkedHashSet<>(request.dishIds());
        List<Dish> dishesToAssociate = validateAndGetDishes(restaurantId, uniqueDishIds);

        short visualOrder = (short) dishModifierRepository.findByModifierIdWithDish(modifier.getId()).size();
        for (Dish dish : dishesToAssociate) {
            if (!dishModifierRepository.existsByIdDishIdAndIdModifierId(dish.getId(), modifier.getId())) {
                DishModifier dishModifier = new DishModifier(dish, modifier, false, (short) 1, visualOrder++);
                dishModifierRepository.save(dishModifier);
            }
        }

        return new ModifierUpdateResponse(
                "Platillos asociados exitosamente al modificador",
                mapToResponse(modifier)
        );
    }

    /**
     * Desactiva un modificador impidiendo su uso en nuevas comandas y conservando el historial de ventas anteriores.
     *
     * @param id Identificador único del modificador a desactivar
     * @return Confirmación y detalle del modificador desactivado
     */
    @Transactional
    public ModifierDeactivationResponse deactivateModifier(Long id) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador a desactivar"
            );
        }

        Modifier modifier = modifierRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + id
                ));

        if (!modifier.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "modifier_already_inactive",
                    "Modificador ya desactivado",
                    "El modificador seleccionado ya se encuentra desactivado"
            );
        }

        modifier.setActive(false);
        Modifier saved = modifierRepository.save(modifier);

        return new ModifierDeactivationResponse(
                "Modificador desactivado exitosamente",
                mapToResponse(saved)
        );
    }

    /**
     * Consulta el detalle de un modificador por su identificador.
     *
     * @param id Identificador único del modificador
     * @return Detalle del modificador y sus platillos asociados
     */
    @Transactional(readOnly = true)
    public ModifierResponse getModifierById(Long id) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        if (id == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador"
            );
        }

        Modifier modifier = modifierRepository.findByIdAndRestaurantId(id, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + id
                ));

        return mapToResponse(modifier);
    }

    /**
     * Consulta el listado de modificadores registrados con filtros opcionales.
     *
     * @param dishId Filtro opcional por platillo asociado
     * @param search Filtro opcional de búsqueda por nombre o código
     * @param active Filtro opcional por estado activo
     * @return Listado de modificadores con sus platillos asociados
     */
    @Transactional(readOnly = true)
    public List<ModifierResponse> listModifiers(Long dishId, String search, Boolean active) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        List<Modifier> modifiers;
        if (dishId != null) {
            modifiers = modifierRepository.findByRestaurantIdAndDishIdAndActive(restaurantId, dishId, active);
        } else {
            modifiers = modifierRepository.findByRestaurantIdAndSearchAndActive(restaurantId, pattern, active);
        }

        return modifiers.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Consulta los modificadores asignados a un platillo específico.
     *
     * @param dishId Identificador único del platillo
     * @return Listado de modificadores asociados al platillo
     */
    @Transactional(readOnly = true)
    public List<ModifierResponse> listModifiersByDishId(Long dishId) {
        return listModifiers(dishId, null, null);
    }

    private List<Dish> validateAndGetDishes(Long restaurantId, Set<Long> dishIds) {
        List<Dish> dishes = new ArrayList<>();
        for (Long dishId : dishIds) {
            Dish dish = dishRepository.findByIdAndRestaurantId(dishId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "dish_not_found",
                            "Platillo no encontrado",
                            "No se encontró un platillo con el identificador " + dishId
                    ));
            dishes.add(dish);
        }
        return dishes;
    }

    private String resolveModifierCode(Long restaurantId, String providedCode) {
        if (providedCode != null && !providedCode.isBlank()) {
            String sanitized = providedCode.trim().toUpperCase();
            if (modifierRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, sanitized)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "duplicate_modifier_code",
                        "Código duplicado",
                        "Ya existe un modificador registrado con el código '" + sanitized + "'"
                );
            }
            return sanitized;
        }

        long nextIndex = modifierRepository.countByRestaurantId(restaurantId) + 1;
        String generated = String.format("MOD-%04d", nextIndex);
        while (modifierRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, generated)) {
            nextIndex++;
            generated = String.format("MOD-%04d", nextIndex);
        }
        return generated;
    }

    private ModifierResponse mapToResponse(Modifier modifier) {
        List<DishModifier> associations = dishModifierRepository.findByModifierIdWithDish(modifier.getId());

        List<ModifierDishItemResponse> dishItems = associations.stream()
                .map(assoc -> new ModifierDishItemResponse(
                        assoc.getDish().getId(),
                        assoc.getDish().getCode(),
                        assoc.getDish().getName(),
                        assoc.getDish().getCategory() != null ? assoc.getDish().getCategory().getName() : null,
                        assoc.isRequired(),
                        assoc.getMaxSelections()
                ))
                .toList();

        return new ModifierResponse(
                modifier.getId(),
                modifier.getCode(),
                modifier.getName(),
                modifier.getDescription(),
                modifier.getAdditionalPrice(),
                modifier.isActive(),
                modifierRecipeVersionRepository.existsActiveByModifierId(modifier.getId()),
                dishItems,
                modifier.getCreatedAt(),
                modifier.getUpdatedAt()
        );
    }
}
