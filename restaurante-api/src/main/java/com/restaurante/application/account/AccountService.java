package com.restaurante.application.account;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.AccountMerge;
import com.restaurante.domain.model.AccountTransfer;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Role;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.AccountMergeRepository;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.AccountTransferRepository;
import com.restaurante.domain.repository.ComandaDetailRepository;
import com.restaurante.domain.repository.ComandaRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.RoleRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.account.ActiveFusionResponse;
import com.restaurante.web.dto.account.MergeAccountsRequest;
import com.restaurante.web.dto.account.MergeAccountsResponse;
import com.restaurante.web.dto.account.OpenAccountRequest;
import com.restaurante.web.dto.account.RequestBillResponse;
import com.restaurante.web.dto.account.TransferAccountRequest;
import com.restaurante.web.dto.account.TransferAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio de aplicación para la gestión, apertura, transferencia, fusión y cobro de cuentas de consumo en mesas del restaurante.
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
    private final AccountMergeRepository accountMergeRepository;
    private final ComandaDetailRepository comandaDetailRepository;
    private final NotificationRepository notificationRepository;
    private final RoleRepository roleRepository;

    public AccountService(
            AccountRepository accountRepository,
            RestaurantTableRepository tableRepository,
            ReservationRepository reservationRepository,
            RestaurantUserProfileRepository userProfileRepository,
            AccountTransferRepository accountTransferRepository,
            ComandaRepository comandaRepository,
            AccountMergeRepository accountMergeRepository,
            ComandaDetailRepository comandaDetailRepository,
            NotificationRepository notificationRepository,
            RoleRepository roleRepository) {
        this.accountRepository = accountRepository;
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.userProfileRepository = userProfileRepository;
        this.accountTransferRepository = accountTransferRepository;
        this.comandaRepository = comandaRepository;
        this.accountMergeRepository = accountMergeRepository;
        this.comandaDetailRepository = comandaDetailRepository;
        this.notificationRepository = notificationRepository;
        this.roleRepository = roleRepository;
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

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsReadyForPayment(
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);

        List<Account> accounts = accountRepository
                .findReadyForPaymentByRestaurantId(context.restaurantId());

        return accounts.stream()
                .map(account -> {
                    RestaurantTable table = tableRepository
                            .findByIdAndRestaurantId(
                                    account.getTableId(),
                                    context.restaurantId()
                            )
                            .orElse(null);

                    String waiterName =
                            resolveUserName(account.getWaiterId());

                    return toResponse(
                            account,
                            table,
                            waiterName
                    );
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

        // Validar que la mesa destino tenga capacidad suficiente para las personas de la cuenta
        if (destinationTable.getCapacity() < account.getPeopleCount()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "destination_table_insufficient_capacity",
                    "Capacidad insuficiente",
                    "La mesa destino no tiene capacidad suficiente para las "
                            + account.getPeopleCount() + " personas de la cuenta"
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

    /**
     * Fusiona dos cuentas de mesas identificando la cuenta origen por su ID.
     */
    @Transactional
    public MergeAccountsResponse mergeAccountsById(
            Long accountId,
            MergeAccountsRequest request,
            Authentication authentication) {
        return mergeAccounts(accountId, false, request, authentication);
    }

    /**
     * Fusiona dos cuentas de mesas identificando la cuenta origen por el ID de la mesa.
     */
    @Transactional
    public MergeAccountsResponse mergeAccountsByTable(
            Long originTableId,
            MergeAccountsRequest request,
            Authentication authentication) {
        return mergeAccounts(originTableId, true, request, authentication);
    }

    /**
     * Fusiona dos cuentas abiertas de mesas que se unieron físicamente.
     * Combina el detalle y comandas de ambas en una sola cuenta (destino) y libera una de las mesas (origen).
     * Si alguna de las cuentas está en proceso de cobro ("LISTA_COBRO"), impide la fusión.
     */
    @Transactional
    public MergeAccountsResponse mergeAccounts(
            Long originId,
            boolean isOriginTableId,
            MergeAccountsRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        // 1. Resolver cuenta origen
        Account originAccount;
        if (isOriginTableId) {
            originAccount = accountRepository
                    .findActiveByTableIdAndRestaurantId(originId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "origin_active_account_not_found",
                            "Sin cuenta activa",
                            "La mesa de origen no tiene una cuenta activa para fusionar"
                    ));
        } else if (originId != null) {
            originAccount = accountRepository
                    .findByIdAndRestaurantId(originId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "origin_account_not_found",
                            "Cuenta origen no encontrada",
                            "La cuenta origen solicitada no existe"
                    ));
        } else if (request != null && request.cuentaOrigenId() != null) {
            originAccount = accountRepository
                    .findByIdAndRestaurantId(request.cuentaOrigenId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "origin_account_not_found",
                            "Cuenta origen no encontrada",
                            "La cuenta origen solicitada no existe"
                    ));
        } else if (request != null && request.mesaOrigenId() != null) {
            originAccount = accountRepository
                    .findActiveByTableIdAndRestaurantId(request.mesaOrigenId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "origin_active_account_not_found",
                            "Sin cuenta activa",
                            "La mesa de origen no tiene una cuenta activa para fusionar"
                    ));
        } else {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_origin_account",
                    "Cuenta origen requerida",
                    "Debe especificar la cuenta o mesa origen para la fusión"
            );
        }

        // 2. Resolver cuenta destino
        Account destinationAccount;
        if (request != null && request.cuentaDestinoId() != null) {
            destinationAccount = accountRepository
                    .findByIdAndRestaurantId(request.cuentaDestinoId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "destination_account_not_found",
                            "Cuenta destino no encontrada",
                            "La cuenta destino solicitada no existe"
                    ));
        } else if (request != null && request.mesaDestinoId() != null) {
            destinationAccount = accountRepository
                    .findActiveByTableIdAndRestaurantId(request.mesaDestinoId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "destination_active_account_not_found",
                            "Sin cuenta activa destino",
                            "La mesa destino no tiene una cuenta activa para recibir la fusión"
                    ));
        } else {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_destination_account",
                    "Cuenta destino requerida",
                    "Debe especificar la cuenta o mesa destino para la fusión"
            );
        }

        // 3. Validar que no sea la misma cuenta
        if (originAccount.getId().equals(destinationAccount.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "same_account_merge",
                    "Cuentas idénticas",
                    "Una cuenta no puede fusionarse consigo misma"
            );
        }

        // 4. Validar que pertenezcan a mesas distintas
        if (originAccount.getTableId().equals(destinationAccount.getTableId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "same_table_merge",
                    "Mesas idénticas",
                    "No se pueden fusionar cuentas de la misma mesa"
            );
        }

        // 5. Validar si alguna de las cuentas está lista para cobro o en proceso de facturación
        boolean isOriginInBilling = "LISTA_COBRO".equalsIgnoreCase(originAccount.getStatus())
                || originAccount.getRequestedPaymentAt() != null
                || "PARCIALMENTE_PAGADA".equalsIgnoreCase(originAccount.getStatus());
        boolean isDestInBilling = "LISTA_COBRO".equalsIgnoreCase(destinationAccount.getStatus())
                || destinationAccount.getRequestedPaymentAt() != null
                || "PARCIALMENTE_PAGADA".equalsIgnoreCase(destinationAccount.getStatus());

        if (isOriginInBilling || isDestInBilling) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_in_billing",
                    "Cuenta en proceso de cobro",
                    "La cuenta ya está en proceso de cobro"
            );
        }

        // 6. Validar que ambas cuentas estén en estado 'ABIERTA'
        if (!"ABIERTA".equalsIgnoreCase(originAccount.getStatus())
                || !"ABIERTA".equalsIgnoreCase(destinationAccount.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta no válido",
                    "Solo se pueden fusionar cuentas en estado abierta"
            );
        }

        // 7. Cargar y validar mesas correspondientes
        RestaurantTable originTable = tableRepository
                .findByIdAndRestaurantId(originAccount.getTableId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "origin_table_not_found",
                        "Mesa origen no encontrada",
                        "La mesa origen de la cuenta no existe"
                ));

        RestaurantTable destinationTable = tableRepository
                .findByIdAndRestaurantId(destinationAccount.getTableId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "destination_table_not_found",
                        "Mesa destino no encontrada",
                        "La mesa destino de la cuenta no existe"
                ));

        // 7.1 Validar que ninguna de las dos mesas ya forme parte de una fusión activa
        // (evita re-seleccionar una mesa ya fusionada y limita la fusión a 2 mesas, sin cadenas A->B->C)
        Set<Long> activeFusedTableIds = findActiveFusedTableIds(restaurantId);
        if (activeFusedTableIds.contains(originTable.getId()) || activeFusedTableIds.contains(destinationTable.getId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_already_fused",
                    "Mesa ya fusionada",
                    "Una de las mesas seleccionadas ya forma parte de una fusión activa"
            );
        }

        // 8. Contar las comandas que se trasladarán (el trigger de BD `fn_ejecutar_fusion_cuenta`
        // es quien las reasigna físicamente a la cuenta destino al insertar la fusión)
        int comandasTransferidas = comandaRepository.findByAccountId(originAccount.getId()).size();

        // 9. Total de personas de la cuenta unificada (el trigger de BD suma esto sobre la
        // cuenta destino; aquí solo se calcula para la respuesta, sin escribirlo por JPA)
        short totalPeople = (short) (destinationAccount.getPeopleCount() + originAccount.getPeopleCount());

        // 10. Registrar la fusión: este INSERT dispara los triggers de BD que realizan la
        // fusión real (`fn_ejecutar_fusion_cuenta`, `fn_sincronizar_estado_mesa`):
        // trasladan las comandas, suman las personas, marcan la cuenta origen como 'FUSIONADA'
        // y mantienen AMBAS mesas en estado OCUPADA (vía cuenta_mesas). No se debe volver a
        // actualizar la cuenta origen por JPA después de esto: `fn_proteger_cuenta_terminal`
        // bloquea cualquier UPDATE sobre una cuenta ya FUSIONADA/CERRADA/CANCELADA.
        String reason = request != null && request.motivo() != null && !request.motivo().trim().isEmpty()
                ? request.motivo().trim()
                : "Mesas unidas físicamente";

        AccountMerge merge = new AccountMerge(
                originAccount.getId(),
                destinationAccount.getId(),
                context.userId(),
                reason
        );
        AccountMerge savedMerge = accountMergeRepository.save(merge);

        int totalComandasDestino = comandaRepository.findByAccountId(destinationAccount.getId()).size();

        return new MergeAccountsResponse(
                "Cuentas fusionadas exitosamente en una sola cuenta",
                savedMerge.getId(),
                originAccount.getId(),
                originAccount.getAccountNumber(),
                "FUSIONADA",
                originTable.getId(),
                originTable.getNumber(),
                originTable.getStatus(),
                destinationAccount.getId(),
                destinationAccount.getAccountNumber(),
                destinationAccount.getStatus(),
                destinationTable.getId(),
                destinationTable.getNumber(),
                destinationTable.getStatus(),
                comandasTransferidas,
                totalComandasDestino,
                totalPeople,
                context.userId(),
                reason,
                savedMerge.getPerformedAt() != null ? savedMerge.getPerformedAt() : Instant.now()
        );
    }

    /**
     * Lista las fusiones de mesas actualmente vigentes (la cuenta destino sigue activa).
     */
    @Transactional(readOnly = true)
    public List<ActiveFusionResponse> getActiveFusions(Authentication authentication) {
        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        List<AccountMerge> merges = accountMergeRepository.findActiveFusionsByRestaurant(restaurantId);
        List<ActiveFusionResponse> result = new ArrayList<>();

        for (AccountMerge merge : merges) {
            Optional<Account> originAccount = accountRepository.findById(merge.getOriginAccountId());
            Optional<Account> destinationAccount = accountRepository.findById(merge.getDestinationAccountId());

            if (originAccount.isEmpty() || destinationAccount.isEmpty()) {
                continue;
            }

            RestaurantTable originTable = tableRepository.findById(originAccount.get().getTableId()).orElse(null);
            RestaurantTable destinationTable = tableRepository.findById(destinationAccount.get().getTableId()).orElse(null);

            result.add(new ActiveFusionResponse(
                    originAccount.get().getTableId(),
                    originTable != null ? originTable.getNumber() : "",
                    destinationAccount.get().getTableId(),
                    destinationTable != null ? destinationTable.getNumber() : "",
                    destinationAccount.get().getId(),
                    destinationAccount.get().getPeopleCount()
            ));
        }

        return result;
    }

    /**
     * Resuelve el conjunto de mesas que actualmente forman parte de una fusión vigente (origen o destino).
     */
    private Set<Long> findActiveFusedTableIds(Long restaurantId) {
        List<AccountMerge> merges = accountMergeRepository.findActiveFusionsByRestaurant(restaurantId);
        Set<Long> tableIds = new HashSet<>();

        for (AccountMerge merge : merges) {
            accountRepository.findById(merge.getOriginAccountId())
                    .ifPresent(account -> tableIds.add(account.getTableId()));
            accountRepository.findById(merge.getDestinationAccountId())
                    .ifPresent(account -> tableIds.add(account.getTableId()));
        }

        return tableIds;
    }

    /**
     * Marca la cuenta como lista para cobro identificándola por su ID.
     */
    @Transactional
    public RequestBillResponse requestBillById(Long accountId, Authentication authentication) {
        return requestBill(accountId, false, authentication);
    }

    /**
     * Marca la cuenta activa de una mesa como lista para cobro identificándola por el ID de la mesa.
     */
    @Transactional
    public RequestBillResponse requestBillByTable(Long tableId, Authentication authentication) {
        return requestBill(tableId, true, authentication);
    }

    /**
     * Marca una cuenta abierta como lista para cobro, cambia el estado de la mesa a 'CUENTA_SOLICITADA'
     * y genera la notificación correspondiente para el cajero.
     * Si la cuenta no tiene platillos registrados, la acción es impedida.
     */
    @Transactional
    public RequestBillResponse requestBill(
            Long accountIdOrTableId,
            boolean isTableId,
            Authentication authentication) {

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
                            "La mesa no tiene una cuenta activa para solicitar cobro"
                    ));
        } else {
            account = accountRepository
                    .findByIdAndRestaurantId(accountIdOrTableId, restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "account_not_found",
                            "Cuenta no encontrada",
                            "La cuenta solicitada no existe"
                    ));
        }

        if ("LISTA_COBRO".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_already_in_billing",
                    "Cuenta ya en cobro",
                    "La cuenta ya se encuentra marcada como lista para cobro"
            );
        }

        if (!"ABIERTA".equalsIgnoreCase(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invalid_account_status",
                    "Estado de cuenta inválido",
                    "Solo se pueden marcar como listas para cobro cuentas en estado abierta"
            );
        }

        // Validar que la cuenta tenga al menos un platillo registrado
        long activeDishesCount = comandaDetailRepository.countActiveByAccountId(account.getId());
        if (activeDishesCount <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "account_has_no_dishes",
                    "Cuenta sin platillos registrados",
                    "No se puede marcar como lista para cobro una cuenta sin platillos registrados"
            );
        }

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(account.getTableId(), restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "La mesa asociada a la cuenta no existe"
                ));

        // 1. Marcar la cuenta como LISTA_COBRO
        account.setStatus("LISTA_COBRO");
        Instant now = Instant.now();
        account.setRequestedPaymentAt(now);
        Account savedAccount = accountRepository.save(account);

        // 2. Cambiar el estado de la mesa a 'CUENTA_SOLICITADA'
        table.setStatus(TableStatus.CUENTA_SOLICITADA);
        RestaurantTable savedTable = tableRepository.save(table);

        // 3. Notificar al cajero
        Role cashierRole = roleRepository.findByName(RoleName.CASHIER).orElse(null);
        Long cashierRoleId = cashierRole != null ? cashierRole.getId() : null;

        String tableNumber = savedTable.getNumber();
        Notification notification = new Notification(
                restaurantId,
                null,
                cashierRoleId,
                "CUENTA_LISTA_COBRO",
                "Cuenta lista para cobro - Mesa " + tableNumber,
                "La cuenta " + savedAccount.getAccountNumber() + " de la mesa " + tableNumber + " está lista para cobro.",
                "CUENTA",
                savedAccount.getId().toString(),
                "ALTA"
        );
        Notification savedNotification = notificationRepository.save(notification);

        return new RequestBillResponse(
                "Cuenta marcada exitosamente como lista para cobro",
                savedAccount.getId(),
                savedAccount.getAccountNumber(),
                savedAccount.getStatus(),
                savedAccount.getRequestedPaymentAt(),
                savedTable.getId(),
                savedTable.getNumber(),
                savedTable.getStatus(),
                activeDishesCount,
                true,
                savedNotification.getId()
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
