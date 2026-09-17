package com.restaurante.application.account;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.ComandaDetailModifier;
import com.restaurante.domain.model.ComandaDetailStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Subaccount;
import com.restaurante.domain.model.SubaccountDetail;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.SubaccountDetailRepository;
import com.restaurante.domain.repository.SubaccountRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.subaccount.AssignItemRequest;
import com.restaurante.web.dto.subaccount.SplitAccountResponse;
import com.restaurante.web.dto.subaccount.SplitByItemsRequest;
import com.restaurante.web.dto.subaccount.SplitByPeopleRequest;
import com.restaurante.web.dto.subaccount.SubaccountItemDefinitionRequest;
import com.restaurante.web.dto.subaccount.SubaccountItemResponse;
import com.restaurante.web.dto.subaccount.SubaccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de aplicación para la división de cuentas abiertas por número de personas o por ítems específicos.
 */
@Service
public class SubaccountService {

    private final AccountRepository accountRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaDetailRepository comandaDetailRepository;
    private final SubaccountRepository subaccountRepository;
    private final SubaccountDetailRepository subaccountDetailRepository;
    private final RestaurantUserProfileRepository userProfileRepository;

    public SubaccountService(
            AccountRepository accountRepository,
            RestaurantTableRepository tableRepository,
            ComandaRepository comandaRepository,
            ComandaDetailRepository comandaDetailRepository,
            SubaccountRepository subaccountRepository,
            SubaccountDetailRepository subaccountDetailRepository,
            RestaurantUserProfileRepository userProfileRepository) {
        this.accountRepository = accountRepository;
        this.comandaRepository = comandaRepository;
        this.comandaDetailRepository = comandaDetailRepository;
        this.subaccountRepository = subaccountRepository;
        this.subaccountDetailRepository = subaccountDetailRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Divide una cuenta abierta equitativamente entre un número de personas.
     */
    @Transactional
    public SplitAccountResponse splitByPeople(
            Long accountId,
            SplitByPeopleRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = getValidAccountForSplit(accountId, context.restaurantId());

        if (request == null || request.numeroPersonas() == null || request.numeroPersonas() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_people_count",
                    "Número de personas inválido",
                    "La cuenta debe dividirse entre al menos 2 personas"
            );
        }

        int n = request.numeroPersonas();

        // Verificar subcuentas existentes
        cleanExistingSubaccounts(account.getId());

        // Obtener platillos válidos de la cuenta
        List<ComandaDetail> activeDetails = getActiveComandaDetails(account.getId());
        if (activeDetails.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "no_dishes_registered",
                    "Sin platillos registrados",
                    "La cuenta no tiene platillos registrados para dividir"
            );
        }

        // Calcular subtotal general de la cuenta
        BigDecimal totalAccount = BigDecimal.ZERO;
        for (ComandaDetail d : activeDetails) {
            BigDecimal unitPrice = calculateDetailUnitPrice(d);
            totalAccount = totalAccount.add(unitPrice.multiply(BigDecimal.valueOf(d.getQuantity())));
        }

        // Calcular porcentajes exactos (sumando 100.0000%)
        BigDecimal basePercentage = BigDecimal.valueOf(100.0)
                .divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
        BigDecimal sumPercentages = BigDecimal.ZERO;
        List<BigDecimal> percentages = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            percentages.add(basePercentage);
            sumPercentages = sumPercentages.add(basePercentage);
        }
        percentages.add(BigDecimal.valueOf(100.0).subtract(sumPercentages));

        List<SubaccountResponse> subaccountResponses = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            short subNum = (short) (i + 1);
            String name = (request.nombres() != null && i < request.nombres().size()
                    && request.nombres().get(i) != null && !request.nombres().get(i).isBlank())
                    ? request.nombres().get(i).trim()
                    : "Persona " + subNum;

            BigDecimal percentage = percentages.get(i);
            BigDecimal subtotal = totalAccount.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            Subaccount sub = new Subaccount(account, subNum, name, "PERSONAS", percentage);
            sub.setSubtotalSnapshot(subtotal);
            Subaccount savedSub = subaccountRepository.save(sub);

            List<SubaccountItemResponse> itemResponses = new ArrayList<>();
            for (ComandaDetail d : activeDetails) {
                BigDecimal assignedQty = BigDecimal.valueOf(d.getQuantity())
                        .multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

                SubaccountDetail sd = new SubaccountDetail(savedSub, d, assignedQty);
                SubaccountDetail savedSd = subaccountDetailRepository.save(sd);
                savedSub.getDetails().add(savedSd);

                BigDecimal unitPrice = calculateDetailUnitPrice(d);
                BigDecimal itemSubtotal = unitPrice.multiply(assignedQty).setScale(2, RoundingMode.HALF_UP);

                itemResponses.add(new SubaccountItemResponse(
                        savedSd.getId(),
                        d.getId(),
                        d.getNameSnapshot(),
                        assignedQty,
                        unitPrice,
                        itemSubtotal
                ));
            }

            subaccountResponses.add(toSubaccountResponse(savedSub, itemResponses));
        }

        return new SplitAccountResponse(
                "Cuenta dividida exitosamente por personas",
                account.getId(),
                account.getAccountNumber(),
                "PERSONAS",
                totalAccount.setScale(2, RoundingMode.HALF_UP),
                subaccountResponses.size(),
                subaccountResponses
        );
    }

    /**
     * Divide una cuenta abierta asignando ítems específicos a distintas subcuentas.
     */
    @Transactional
    public SplitAccountResponse splitByItems(
            Long accountId,
            SplitByItemsRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = getValidAccountForSplit(accountId, context.restaurantId());

        if (request == null || request.subcuentas() == null || request.subcuentas().size() < 2) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_subaccounts_count",
                    "Cantidad de subcuentas inválida",
                    "Debe especificar al menos dos subcuentas para la división"
            );
        }

        // Validar que no haya doble asignación de ítems entre las subcuentas solicitadas
        Set<Long> assignedDetailIds = new HashSet<>();
        for (SubaccountItemDefinitionRequest subDef : request.subcuentas()) {
            if (subDef.items() == null || subDef.items().isEmpty()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "empty_subaccount_items",
                        "Subcuenta sin platillos",
                        "Cada subcuenta debe incluir al menos un platillo asignado"
                );
            }
            for (AssignItemRequest itemReq : subDef.items()) {
                if (itemReq.comandaDetalleId() == null) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "missing_item_id",
                            "Ítem no especificado",
                            "El identificador del platillo es requerido"
                    );
                }
                if (assignedDetailIds.contains(itemReq.comandaDetalleId())) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "item_already_assigned",
                            "Platillo ya asignado",
                            "El platillo ya fue asignado a otra sub-cuenta"
                    );
                }
                assignedDetailIds.add(itemReq.comandaDetalleId());
            }
        }

        // Limpiar subcuentas previas si estaban pendientes
        cleanExistingSubaccounts(account.getId());

        List<SubaccountResponse> subaccountResponses = new ArrayList<>();
        BigDecimal globalSubtotal = BigDecimal.ZERO;

        for (int i = 0; i < request.subcuentas().size(); i++) {
            SubaccountItemDefinitionRequest subDef = request.subcuentas().get(i);
            short subNum = (short) (i + 1);
            String name = subDef.nombre() != null && !subDef.nombre().isBlank()
                    ? subDef.nombre().trim()
                    : "Subcuenta " + subNum;

            Subaccount sub = new Subaccount(account, subNum, name, "ITEMS", null);
            Subaccount savedSub = subaccountRepository.save(sub);

            BigDecimal subSubtotal = BigDecimal.ZERO;
            List<SubaccountItemResponse> itemResponses = new ArrayList<>();

            for (AssignItemRequest itemReq : subDef.items()) {
                ComandaDetail cd = comandaDetailRepository.findById(itemReq.comandaDetalleId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "dish_not_found",
                                "Platillo no encontrado",
                                "El platillo con ID " + itemReq.comandaDetalleId() + " no existe"
                        ));

                if (!cd.getComanda().getAccount().getId().equals(account.getId())) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "dish_not_in_account",
                            "Platillo no pertenece a la cuenta",
                            "El platillo seleccionado no pertenece a la cuenta a dividir"
                    );
                }

                if (cd.getStatus() == ComandaDetailStatus.CANCELADO || cd.getStatus() == ComandaDetailStatus.NO_DISPONIBLE) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "dish_unavailable",
                            "Platillo no disponible",
                            "El platillo seleccionado se encuentra cancelado o no disponible"
                    );
                }

                BigDecimal qty = itemReq.cantidad() != null
                        ? itemReq.cantidad()
                        : BigDecimal.valueOf(cd.getQuantity());

                if (qty.compareTo(BigDecimal.ZERO) <= 0 || qty.compareTo(BigDecimal.valueOf(cd.getQuantity())) > 0) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "invalid_assigned_quantity",
                            "Cantidad inválida",
                            "La cantidad asignada no puede superar la cantidad del platillo"
                    );
                }

                SubaccountDetail sd = new SubaccountDetail(savedSub, cd, qty);
                SubaccountDetail savedSd = subaccountDetailRepository.save(sd);
                savedSub.getDetails().add(savedSd);

                BigDecimal unitPrice = calculateDetailUnitPrice(cd);
                BigDecimal itemSubtotal = unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
                subSubtotal = subSubtotal.add(itemSubtotal);

                itemResponses.add(new SubaccountItemResponse(
                        savedSd.getId(),
                        cd.getId(),
                        cd.getNameSnapshot(),
                        qty,
                        unitPrice,
                        itemSubtotal
                ));
            }

            savedSub.setSubtotalSnapshot(subSubtotal.setScale(2, RoundingMode.HALF_UP));
            subaccountRepository.save(savedSub);
            globalSubtotal = globalSubtotal.add(subSubtotal);

            subaccountResponses.add(toSubaccountResponse(savedSub, itemResponses));
        }

        return new SplitAccountResponse(
                "Cuenta dividida exitosamente por ítems específicos",
                account.getId(),
                account.getAccountNumber(),
                "ITEMS",
                globalSubtotal.setScale(2, RoundingMode.HALF_UP),
                subaccountResponses.size(),
                subaccountResponses
        );
    }

    /**
     * Asigna un ítem específico a una sub-cuenta existente.
     * Si el ítem ya fue asignado a otra sub-cuenta de la misma cuenta, impide la doble asignación.
     */
    @Transactional
    public SubaccountResponse assignItemToSubaccount(
            Long accountId,
            Long subaccountId,
            AssignItemRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = getValidAccountForSplit(accountId, context.restaurantId());

        Subaccount subaccount = subaccountRepository.findById(subaccountId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "subaccount_not_found",
                        "Subcuenta no encontrada",
                        "La subcuenta solicitada no existe"
                ));

        if (!subaccount.getAccount().getId().equals(account.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "subaccount_account_mismatch",
                    "Subcuenta no corresponde a la cuenta",
                    "La subcuenta no pertenece a la cuenta indicada"
            );
        }

        if (!"PENDIENTE".equals(subaccount.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "subaccount_not_modifiable",
                    "Subcuenta no modificable",
                    "Solo se pueden modificar subcuentas en estado PENDIENTE"
            );
        }

        if (!"ITEMS".equals(subaccount.getSplitType())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_split_type_for_item_assignment",
                    "Tipo de división no compatible",
                    "Solo se pueden asignar ítems individuales a subcuentas divididas por ITEMS"
            );
        }

        if (request == null || request.comandaDetalleId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_item_id",
                    "Platillo requerido",
                    "Debe indicar el identificador del platillo a asignar"
            );
        }

        // Validar si el ítem ya fue asignado a otra sub-cuenta de la misma cuenta
        List<SubaccountDetail> existingAssignments = subaccountDetailRepository
                .findActiveByAccountIdAndComandaDetailId(account.getId(), request.comandaDetalleId());

        for (SubaccountDetail existing : existingAssignments) {
            if (!existing.getSubaccount().getId().equals(subaccountId)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "item_already_assigned",
                        "Platillo ya asignado",
                        "El platillo ya fue asignado a otra sub-cuenta"
                );
            }
        }

        ComandaDetail cd = comandaDetailRepository.findById(request.comandaDetalleId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "El platillo solicitado no existe"
                ));

        if (!cd.getComanda().getAccount().getId().equals(account.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_not_in_account",
                    "Platillo ajeno a la cuenta",
                    "El platillo no pertenece a la cuenta indicada"
            );
        }

        BigDecimal qty = request.cantidad() != null
                ? request.cantidad()
                : BigDecimal.valueOf(cd.getQuantity());

        if (qty.compareTo(BigDecimal.ZERO) <= 0 || qty.compareTo(BigDecimal.valueOf(cd.getQuantity())) > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_assigned_quantity",
                    "Cantidad inválida",
                    "La cantidad asignada no puede superar la cantidad del platillo"
            );
        }

        SubaccountDetail sd = subaccountDetailRepository
                .findBySubaccountIdAndComandaDetailId(subaccountId, cd.getId())
                .orElseGet(() -> new SubaccountDetail(subaccount, cd, qty));

        sd.setAssignedQuantity(qty);
        subaccountDetailRepository.save(sd);

        // Recalcular subtotal de la subcuenta
        List<SubaccountDetail> allDetails = subaccountDetailRepository.findBySubaccountId(subaccountId);
        BigDecimal newSubtotal = BigDecimal.ZERO;
        List<SubaccountItemResponse> itemResponses = new ArrayList<>();

        for (SubaccountDetail d : allDetails) {
            BigDecimal unitPrice = calculateDetailUnitPrice(d.getComandaDetail());
            BigDecimal itemSub = unitPrice.multiply(d.getAssignedQuantity()).setScale(2, RoundingMode.HALF_UP);
            newSubtotal = newSubtotal.add(itemSub);

            itemResponses.add(new SubaccountItemResponse(
                    d.getId(),
                    d.getComandaDetail().getId(),
                    d.getComandaDetail().getNameSnapshot(),
                    d.getAssignedQuantity(),
                    unitPrice,
                    itemSub
            ));
        }

        subaccount.setSubtotalSnapshot(newSubtotal.setScale(2, RoundingMode.HALF_UP));
        subaccountRepository.save(subaccount);

        return toSubaccountResponse(subaccount, itemResponses);
    }

    /**
     * Consulta todas las subcuentas activas de una cuenta principal.
     */
    @Transactional(readOnly = true)
    public List<SubaccountResponse> getSubaccountsByAccountId(Long accountId, Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = accountRepository.findByIdAndRestaurantId(accountId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta solicitada no existe"
                ));

        List<Subaccount> subaccounts = subaccountRepository.findByAccountIdWithDetails(account.getId());
        return subaccounts.stream()
                .map(s -> {
                    List<SubaccountItemResponse> itemResponses = s.getDetails().stream()
                            .map(d -> {
                                BigDecimal unitPrice = calculateDetailUnitPrice(d.getComandaDetail());
                                BigDecimal itemSub = unitPrice.multiply(d.getAssignedQuantity()).setScale(2, RoundingMode.HALF_UP);
                                return new SubaccountItemResponse(
                                        d.getId(),
                                        d.getComandaDetail().getId(),
                                        d.getComandaDetail().getNameSnapshot(),
                                        d.getAssignedQuantity(),
                                        unitPrice,
                                        itemSub
                                );
                            })
                            .toList();
                    return toSubaccountResponse(s, itemResponses);
                })
                .toList();
    }

    /**
     * Métodos por ID de mesa para comodidad del mesero en sala.
     */
    @Transactional
    public SplitAccountResponse splitByPeopleByTable(
            Long tableId,
            SplitByPeopleRequest request,
            Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = accountRepository.findActiveByTableIdAndRestaurantId(tableId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene una cuenta activa para dividir"
                ));
        return splitByPeople(account.getId(), request, authentication);
    }

    @Transactional
    public SplitAccountResponse splitByItemsByTable(
            Long tableId,
            SplitByItemsRequest request,
            Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = accountRepository.findActiveByTableIdAndRestaurantId(tableId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene una cuenta activa para dividir"
                ));
        return splitByItems(account.getId(), request, authentication);
    }

    @Transactional(readOnly = true)
    public List<SubaccountResponse> getSubaccountsByTableId(Long tableId, Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Account account = accountRepository.findActiveByTableIdAndRestaurantId(tableId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene una cuenta activa"
                ));
        return getSubaccountsByAccountId(account.getId(), authentication);
    }

    @Transactional(readOnly = true)
    public List<SubaccountResponse> getPendingSubaccountsByAccountId(
            Long accountId,
            Authentication authentication) {

        return getSubaccountsByAccountId(
                accountId,
                authentication
        )
                .stream()
                .filter(subaccount ->
                        "PENDIENTE".equals(subaccount.estado()))
                .toList();
    }

    private Account getValidAccountForSplit(Long accountId, Long restaurantId) {
        Account account = accountRepository.findByIdAndRestaurantId(accountId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta solicitada no existe"
                ));

        if ("CERRADA".equalsIgnoreCase(account.getStatus())
                || "CANCELADA".equalsIgnoreCase(account.getStatus())
                || "FUSIONADA".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "terminal_account",
                    "Cuenta terminal",
                    "No se pueden crear o modificar subcuentas de una cuenta terminal"
            );
        }

        return account;
    }

    private void cleanExistingSubaccounts(Long accountId) {
        boolean hasInvoiced = subaccountRepository.existsByAccountIdAndStatus(accountId, "FACTURADA");
        if (hasInvoiced) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "subaccount_already_invoiced",
                    "Subcuentas facturadas",
                    "La cuenta ya tiene subcuentas facturadas y no puede dividirse nuevamente"
            );
        }

        List<Subaccount> pending = subaccountRepository.findByAccountIdAndStatusNot(accountId, "CANCELADA");
        if (!pending.isEmpty()) {
            subaccountRepository.deleteAll(pending);
            subaccountRepository.flush();
        }
    }

    private List<ComandaDetail> getActiveComandaDetails(Long accountId) {
        List<Comanda> comandas = comandaRepository.findByAccountId(accountId);
        List<ComandaDetail> activeDetails = new ArrayList<>();
        for (Comanda comanda : comandas) {
            List<ComandaDetail> details = comandaDetailRepository.findByComandaIdWithModifiers(comanda.getId());
            for (ComandaDetail d : details) {
                if (d.getStatus() != ComandaDetailStatus.CANCELADO && d.getStatus() != ComandaDetailStatus.NO_DISPONIBLE) {
                    activeDetails.add(d);
                }
            }
        }
        return activeDetails;
    }

    private BigDecimal calculateDetailUnitPrice(ComandaDetail detail) {
        BigDecimal basePrice = detail.getUnitPriceSnapshot() != null
                ? detail.getUnitPriceSnapshot()
                : BigDecimal.ZERO;

        BigDecimal modifiersPrice = BigDecimal.ZERO;
        if (detail.getModifiers() != null) {
            for (ComandaDetailModifier mod : detail.getModifiers()) {
                BigDecimal modPrice = mod.getAdditionalPriceSnapshot() != null
                        ? mod.getAdditionalPriceSnapshot()
                        : BigDecimal.ZERO;
                modifiersPrice = modifiersPrice.add(modPrice.multiply(BigDecimal.valueOf(mod.getQuantity())));
            }
        }
        return basePrice.add(modifiersPrice);
    }

    private SubaccountResponse toSubaccountResponse(Subaccount sub, List<SubaccountItemResponse> items) {
        return new SubaccountResponse(
                sub.getId(),
                sub.getAccount().getId(),
                sub.getSubaccountNumber(),
                sub.getName(),
                sub.getSplitType(),
                sub.getAssignedPercentage(),
                sub.getStatus(),
                sub.getSubtotalSnapshot(),
                sub.getCreatedAt(),
                items
        );
    }

    private AuthenticatedUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtData jwtData)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        RestaurantUserProfile profile = userProfileRepository
                .findById(jwtData.userId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        String fullName = (profile.getFirstName() + " " + profile.getLastName()).trim();
        return new AuthenticatedUser(jwtData.userId(), profile.getRestaurantId(), fullName);
    }

    private record AuthenticatedUser(Long userId, Long restaurantId, String fullName) {
    }
}
