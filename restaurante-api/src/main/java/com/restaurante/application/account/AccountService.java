package com.restaurante.application.account;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.AccountTransfer;
import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.AccountTransferRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
import com.restaurante.web.dto.account.TransferAccountRequest;
import com.restaurante.web.dto.account.TransferAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación para la gestión, apertura y transferencia de cuentas de consumo en mesas del restaurante.
 */
@Service
public class AccountService {

    private static final ZoneId GUATEMALA = ZoneId.of("America/Guatemala");

    private final AccountRepository accountRepository;
    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final RestaurantUserProfileRepository userProfileRepository;
    private final AccountTransferRepository accountTransferRepository;
    private final ComandaRepository comandaRepository;

    public AccountService(
            AccountRepository accountRepository,
            RestaurantTableRepository tableRepository,
            ReservationRepository reservationRepository,
            RestaurantUserProfileRepository userProfileRepository,
            AccountTransferRepository accountTransferRepository,
            ComandaRepository comandaRepository) {
        this.accountRepository = accountRepository;
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.userProfileRepository = userProfileRepository;
        this.accountTransferRepository = accountTransferRepository;
        this.comandaRepository = comandaRepository;
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

    /**
     * Transfiere una cuenta abierta a otra mesa identificándola por su ID de cuenta.
     */
    @Transactional
    public TransferAccountResponse transferAccountById(
            Long accountId,
            TransferAccountRequest request,
            Authentication authentication) {
        return transferAccount(accountId, false, request, authentication);
    }

    /**
     * Transfiere la cuenta activa de una mesa a otra mesa identificándola por la mesa origen.
     */
    @Transactional
    public TransferAccountResponse transferAccountByTable(
            Long originTableId,
            TransferAccountRequest request,
            Authentication authentication) {
        return transferAccount(originTableId, true, request, authentication);
    }

    /**
     * Realiza la transferencia de una cuenta abierta a una mesa libre destino.
     * Conserva todas las comandas de la cuenta, libera la mesa origen y ocupa la mesa destino.
     * Si la mesa destino no está en estado "libre" o disponible, impide la transferencia.
     */
    @Transactional
    public TransferAccountResponse transferAccount(
            Long accountIdOrTableId,
            boolean isTableId,
            TransferAccountRequest request,
            Authentication authentication) {

        if (request == null || request.mesaDestinoId() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_destination_table",
                    "Mesa destino requerida",
                    "Debe especificar la mesa destino para la transferencia"
            );
        }

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        Account account;
        if (isTableId) {
            account = accountRepository
                    .findActiveByTableIdAndRestaurantId(accountIdOrTableId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "active_account_not_found",
                            "Sin cuenta activa",
                            "La mesa de origen no tiene una cuenta activa para transferir"
                    ));
        } else {
            account = accountRepository
                    .findByIdAndRestaurantId(accountIdOrTableId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "account_not_found",
                            "Cuenta no encontrada",
                            "La cuenta seleccionada no existe"
                    ));
        }

        if (!"ABIERTA".equals(account.getStatus()) && !"LISTA_COBRO".equals(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta no transferible",
                    "Solo se pueden transferir cuentas en estado activa o abierta"
            );
        }

        Long originTableId = account.getTableId();
        Long destinationTableId = request.mesaDestinoId();

        if (originTableId.equals(destinationTableId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "same_table_transfer",
                    "Mesa destino inválida",
                    "La mesa destino debe ser diferente a la mesa origen"
            );
        }

        RestaurantTable originTable = tableRepository
                .findByIdAndRestaurantId(originTableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "origin_table_not_found",
                        "Mesa origen no encontrada",
                        "La mesa de origen de la cuenta no existe"
                ));

        RestaurantTable destinationTable = tableRepository
                .findByIdAndRestaurantId(destinationTableId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "destination_table_not_found",
                        "Mesa destino no encontrada",
                        "La mesa destino no existe"
                ));

        // Validar que la mesa destino esté activa y en estado 'LIBRE'
        if (!destinationTable.isActive() || destinationTable.getStatus() != TableStatus.LIBRE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "destination_table_not_available",
                    "Mesa destino no disponible",
                    "La mesa destino no está disponible"
            );
        }

        // Validar que la mesa destino no tenga ya una cuenta activa
        Optional<Account> destAccount = accountRepository
                .findActiveByTableIdAndRestaurantId(destinationTable.getId(), restaurantId);
        if (destAccount.isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "destination_table_not_available",
                    "Mesa destino no disponible",
                    "La mesa destino no está disponible"
            );
        }

        // Validar que la mesa destino no esté bloqueada por reservas en la ventana horaria actual
        OffsetDateTime now = OffsetDateTime.now(GUATEMALA);
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForTable(destinationTable.getId(), now.minusMinutes(45), now.plusMinutes(15));
        if (!activeReservations.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "destination_table_not_available",
                    "Mesa destino no disponible",
                    "La mesa destino no está disponible"
            );
        }

        // Reubicar la cuenta en la mesa destino
        account.setTableId(destinationTable.getId());
        Account savedAccount = accountRepository.save(account);

        // La mesa origen debe quedar "libre"
        originTable.setStatus(TableStatus.LIBRE);
        tableRepository.save(originTable);

        // La mesa destino debe quedar "ocupada"
        destinationTable.occupy();
        tableRepository.save(destinationTable);

        // Registrar la auditoría de transferencia
        String reason = request.motivo() != null && !request.motivo().trim().isEmpty()
                ? request.motivo().trim()
                : "Reubicación de cliente";
        AccountTransfer transfer = new AccountTransfer(
                savedAccount.getId(),
                originTable.getId(),
                destinationTable.getId(),
                context.userId(),
                reason
        );
        accountTransferRepository.save(transfer);

        // Conservar comandas ya registradas asociadas a la cuenta
        int totalComandas = comandaRepository.findByAccountId(savedAccount.getId()).size();

        return new TransferAccountResponse(
                "Cuenta transferida exitosamente a la mesa destino",
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                originTable.getId(),
                originTable.getNumber(),
                TableStatus.LIBRE,
                destinationTable.getId(),
                destinationTable.getNumber(),
                TableStatus.OCUPADA,
                totalComandas,
                context.userId(),
                reason,
                Instant.now()
        );
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
