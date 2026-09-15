package com.restaurante.application.account;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación para la gestión y apertura de cuentas de consumo en mesas del restaurante.
 */
@Service
public class AccountService {

    private static final ZoneId GUATEMALA = ZoneId.of("America/Guatemala");

    private final AccountRepository accountRepository;
    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final RestaurantUserProfileRepository userProfileRepository;

    public AccountService(
            AccountRepository accountRepository,
            RestaurantTableRepository tableRepository,
            ReservationRepository reservationRepository,
            RestaurantUserProfileRepository userProfileRepository) {
        this.accountRepository = accountRepository;
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Abre una nueva cuenta asociada a una mesa libre y al mesero autenticado.
     * Registra la fecha/hora exacta de apertura y cambia el estado de la mesa a 'OCUPADA'.
     * Si la mesa ya tiene una cuenta activa o se encuentra ocupada, impide la acción.
     */
    @Transactional
    public AccountResponse openAccount(
            Long tableId,
            OpenAccountRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        Long effectiveTableId = tableId;
        if (effectiveTableId == null && request != null) {
            effectiveTableId = request.mesaId();
        }

        if (effectiveTableId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_table_id",
                    "Mesa no especificada",
                    "Debe indicar la mesa en la que se abrirá la cuenta"
            );
        }

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(effectiveTableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "La mesa seleccionada no existe"
                ));

        if (!table.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_inactive",
                    "Mesa inactiva",
                    "La mesa seleccionada ya no se encuentra activa"
            );
        }

        // Validar si la mesa ya tiene una cuenta activa abierta
        Optional<Account> activeAccountOpt = accountRepository
                .findActiveByTableIdAndRestaurantId(table.getId(), restaurantId);

        if (activeAccountOpt.isPresent()
                || table.getStatus() == TableStatus.OCUPADA
                || table.getStatus() == TableStatus.CUENTA_SOLICITADA) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_account_already_active",
                    "Mesa con cuenta activa",
                    "La mesa ya tiene una cuenta activa"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(GUATEMALA);

        // Si la mesa está reservada para otro horario/cliente, impedir apertura
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForTable(table.getId(), now.minusMinutes(45), now.plusMinutes(15));
        if (!activeReservations.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_reserved_for_another_client",
                    "Mesa reservada",
                    "La mesa está reservada para otro horario/cliente"
            );
        }

        short peopleCount = (request != null && request.cantidadPersonas() != null && request.cantidadPersonas() > 0)
                ? request.cantidadPersonas()
                : (short) 1;

        String accountNumber = generateAccountNumber(restaurantId, table.getNumber());
        Account account = new Account(restaurantId, table.getId(), context.userId(), accountNumber, peopleCount);

        if (request != null) {
            if (request.clienteId() != null) {
                account.setClientId(request.clienteId());
            }
            if (request.observaciones() != null && !request.observaciones().trim().isEmpty()) {
                account.setNotes(request.observaciones().trim());
            }
        }

        Account savedAccount = accountRepository.save(account);

        // Cambiar estado de la mesa a 'OCUPADA'
        table.occupy();
        tableRepository.save(table);

        return toResponse(savedAccount, table, context.fullName());
    }

    /**
     * Consulta la cuenta activa de una mesa.
     */
    @Transactional(readOnly = true)
    public AccountResponse getActiveAccountByTable(Long tableId, Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "La mesa seleccionada no existe"
                ));

        Account account = accountRepository
                .findActiveByTableIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "active_account_not_found",
                        "Sin cuenta activa",
                        "La mesa no tiene ninguna cuenta activa en este momento"
                ));

        String waiterName = resolveUserName(account.getWaiterId());
        return toResponse(account, table, waiterName);
    }

    /**
     * Consulta el detalle de una cuenta por su ID.
     */
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId, Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);

        Account account = accountRepository
                .findByIdAndRestaurantId(accountId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta solicitada no existe"
                ));

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(account.getTableId(), context.restaurantId())
                .orElse(null);

        String waiterName = resolveUserName(account.getWaiterId());
        return toResponse(account, table, waiterName);
    }

    /**
     * Lista todas las cuentas activas del restaurante.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getActiveAccounts(Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);

        List<Account> accounts = accountRepository.findActiveByRestaurantId(context.restaurantId());
        return accounts.stream()
                .map(acc -> {
                    RestaurantTable table = tableRepository
                            .findByIdAndRestaurantId(acc.getTableId(), context.restaurantId())
                            .orElse(null);
                    String waiterName = resolveUserName(acc.getWaiterId());
                    return toResponse(acc, table, waiterName);
                })
                .toList();
    }

    private AccountResponse toResponse(Account account, RestaurantTable table, String waiterName) {
        String tableNumber = table != null ? table.getNumber() : "";
        TableStatus tableStatus = table != null ? table.getStatus() : TableStatus.OCUPADA;

        return new AccountResponse(
                account.getId(),
                account.getRestaurantId(),
                account.getTableId(),
                tableNumber,
                account.getWaiterId(),
                waiterName,
                account.getClientId(),
                account.getReservationId(),
                account.getWaitlistId(),
                account.getAccountNumber(),
                account.getPeopleCount(),
                account.getStatus(),
                account.getOpenedAt(),
                account.getNotes(),
                tableStatus
        );
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userProfileRepository.findById(userId)
                .map(p -> (p.getFirstName() + " " + p.getLastName()).trim())
                .orElse("Usuario #" + userId);
    }

    private String generateAccountNumber(Long restaurantId, String tableNumber) {
        String base = "CTA-" + tableNumber.trim() + "-";
        int suffix = (int) (System.currentTimeMillis() % 100000);
        String candidate = base + suffix;
        while (accountRepository.existsByRestaurantIdAndAccountNumber(restaurantId, candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
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
