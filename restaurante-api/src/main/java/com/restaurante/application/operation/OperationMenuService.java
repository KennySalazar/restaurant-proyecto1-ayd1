package com.restaurante.application.operation;

import com.restaurante.application.combo.ComboService;
import com.restaurante.application.dish.DishService;
import com.restaurante.domain.model.DishModifier;
import com.restaurante.domain.repository.DishModifierRepository;
import com.restaurante.web.dto.combo.ComboResponse;
import com.restaurante.web.dto.dish.DishResponse;
import com.restaurante.web.dto.menu.MenuCatalogResponse;
import com.restaurante.web.dto.menu.MenuComboItemResponse;
import com.restaurante.web.dto.menu.MenuComboResponse;
import com.restaurante.web.dto.menu.MenuDishResponse;
import com.restaurante.web.dto.menu.MenuModifierResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para el catálogo del menú operativo del mesero (platillos con
 * modificadores y combos disponibles), utilizado para armar comandas.
 */
@Service
public class OperationMenuService {

    private final DishService dishService;
    private final ComboService comboService;
    private final DishModifierRepository dishModifierRepository;

    public OperationMenuService(DishService dishService,
                                ComboService comboService,
                                DishModifierRepository dishModifierRepository) {
        this.dishService = dishService;
        this.comboService = comboService;
        this.dishModifierRepository = dishModifierRepository;
    }

    @Transactional(readOnly = true)
    public MenuCatalogResponse getActiveMenu() {
        List<DishResponse> dishes = dishService.listDishes();
        List<MenuDishResponse> menuDishes = withModifiers(dishes);
        List<MenuComboResponse> combos = comboService.listCombos(null, true).stream()
                .filter(this::isWithinValidity)
                .map(this::toMenuCombo)
                .toList();
        return new MenuCatalogResponse(menuDishes, combos);
    }

    private boolean isWithinValidity(ComboResponse combo) {
        Instant now = Instant.now();
        boolean started = combo.startDate() == null || !now.isBefore(combo.startDate());
        boolean notExpired = combo.endDate() == null || !now.isAfter(combo.endDate());
        return started && notExpired;
    }

    private List<MenuDishResponse> withModifiers(List<DishResponse> dishes) {
        if (dishes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> dishIds = dishes.stream()
                .map(DishResponse::id)
                .collect(Collectors.toSet());

        Map<Long, List<MenuModifierResponse>> modifiersByDish = new LinkedHashMap<>();
        for (DishModifier dm : dishModifierRepository.findByDishIdInWithModifier(dishIds)) {
            if (!dm.getModifier().isActive()) {
                continue;
            }
            modifiersByDish.computeIfAbsent(dm.getDish().getId(), key -> new ArrayList<>())
                    .add(toMenuModifier(dm));
        }

        return dishes.stream()
                .map(dish -> new MenuDishResponse(
                        dish.id(),
                        dish.code(),
                        dish.name(),
                        dish.description(),
                        dish.categoryId(),
                        dish.categoryName(),
                        dish.salePrice(),
                        dish.imageUrl(),
                        dish.preparationTimeMinutes(),
                        dish.available(),
                        dish.availablePortions(),
                        dish.unavailabilityReason(),
                        modifiersByDish.getOrDefault(dish.id(), Collections.emptyList())))
                .toList();
    }

    private MenuModifierResponse toMenuModifier(DishModifier dm) {
        return new MenuModifierResponse(
                dm.getModifier().getId(),
                dm.getModifier().getCode(),
                dm.getModifier().getName(),
                dm.getModifier().getAdditionalPrice(),
                dm.isRequired(),
                dm.getMaxSelections());
    }

    private MenuComboResponse toMenuCombo(ComboResponse combo) {
        List<MenuComboItemResponse> items = combo.items() == null
                ? Collections.emptyList()
                : combo.items().stream()
                        .map(item -> new MenuComboItemResponse(
                                item.dishId(),
                                item.dishName(),
                                item.quantity()))
                        .toList();

        boolean available = Boolean.TRUE.equals(combo.manualAvailable())
                && Boolean.TRUE.equals(combo.active());

        return new MenuComboResponse(
                combo.id(),
                combo.code(),
                combo.name(),
                combo.description(),
                combo.salePrice(),
                combo.imageUrl(),
                combo.preparationTimeMinutes(),
                available,
                items);
    }
}