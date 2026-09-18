package com.restaurante.application.payment;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.payment.CustomerPointsResponse;
import com.restaurante.web.dto.payment.CustomerResponse;
import com.restaurante.web.dto.payment.RegisterCustomerRequest;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LoyaltyService {

    private final EntityManager entityManager;
    private final AccountRepository accountRepository;

    public LoyaltyService(
            EntityManager entityManager,
            AccountRepository accountRepository) {
        this.entityManager = entityManager;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public CustomerPointsResponse getCustomerPoints(
            Long customerId,
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);

        List<?> results = entityManager
                .createNativeQuery("""
                SELECT
                    c.id,
                    c.nombres,
                    c.apellidos,
                    c.saldo_puntos,
                    cr.valor_monetario_punto
                FROM restaurante.clientes c
                JOIN restaurante.configuraciones_restaurante cr
                  ON cr.restaurante_id = c.restaurante_id
                 AND cr.estado = 'VIGENTE'
                WHERE c.id = :customerId
                  AND c.restaurante_id = :restaurantId
                  AND c.activo = TRUE
                """)
                .setParameter("customerId", customerId)
                .setParameter("restaurantId", restaurantId)
                .getResultList();

        if (results.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "customer_not_found",
                    "Cliente no encontrado",
                    "El cliente indicado no existe o no se encuentra activo"
            );
        }

        Object[] row = (Object[]) results.get(0);

        Long points = ((Number) row[3]).longValue();
        BigDecimal pointValue = (BigDecimal) row[4];

        BigDecimal monetaryValue = pointValue
                .multiply(BigDecimal.valueOf(points))
                .setScale(2, RoundingMode.HALF_UP);

        return new CustomerPointsResponse(
                ((Number) row[0]).longValue(),
                row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                points,
                pointValue,
                monetaryValue
        );
    }

    @Transactional(readOnly = true)
    public CustomerResponse findCustomerByPhone(
            String phone,
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);
        String normalizedPhone = normalizeRequired(phone);

        return findActiveCustomerByPhone(
                restaurantId,
                normalizedPhone
        );
    }

    @Transactional
    public CustomerResponse registerAndAssociateCustomer(
            Long accountId,
            RegisterCustomerRequest request,
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);
        Account account = requireAccountForAssociation(
                accountId,
                restaurantId
        );

        if (account.getClientId() != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_customer_already_associated",
                    "Cuenta con cliente registrado",
                    "La cuenta ya se encuentra asociada a un cliente registrado"
            );
        }

        String names = normalizeRequired(request.nombres());
        String lastNames = normalizeOptional(request.apellidos());
        String phone = normalizeRequired(request.telefono());
        String email = normalizeOptional(request.correo());

        if (customerPhoneExists(restaurantId, phone)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "customer_phone_already_registered",
                    "Telefono ya registrado",
                    "Ya existe un cliente registrado con ese numero de telefono"
            );
        }

        if (email != null && customerEmailExists(restaurantId, email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "customer_email_already_registered",
                    "Correo ya registrado",
                    "Ya existe un cliente registrado con ese correo electronico"
            );
        }

        Object customerIdResult = entityManager
                .createNativeQuery("""
                        INSERT INTO restaurante.clientes (
                            restaurante_id,
                            nombres,
                            apellidos,
                            telefono,
                            correo
                        )
                        VALUES (
                            :restaurantId,
                            :names,
                            :lastNames,
                            :phone,
                            :email
                        )
                        RETURNING id
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("names", names)
                .setParameter("lastNames", lastNames)
                .setParameter("phone", phone)
                .setParameter("email", email)
                .getSingleResult();

        Long customerId = ((Number) customerIdResult).longValue();

        account.setClientId(customerId);
        accountRepository.save(account);

        return findActiveCustomerById(
                restaurantId,
                customerId
        );
    }

    @Transactional
    public CustomerResponse associateCustomer(
            Long accountId,
            Long customerId,
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);
        Account account = requireAccountForAssociation(
                accountId,
                restaurantId
        );

        CustomerResponse customer = findActiveCustomerById(
                restaurantId,
                customerId
        );

        if (account.getClientId() != null) {
            if (account.getClientId().equals(customerId)) {
                return customer;
            }

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_customer_already_associated",
                    "Cuenta con cliente registrado",
                    "La cuenta ya se encuentra asociada a otro cliente registrado"
            );
        }

        account.setClientId(customerId);
        accountRepository.save(account);

        return customer;
    }

    private Account requireAccountForAssociation(
            Long accountId,
            Long restaurantId) {

        Account account = accountRepository
                .findByIdAndRestaurantId(accountId, restaurantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta indicada no existe"
                ));

        if (!"LISTA_COBRO".equals(account.getStatus())
                && !"PARCIALMENTE_PAGADA".equals(account.getStatus())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_not_ready_for_customer_association",
                    "Cuenta no disponible",
                    "El cliente solo puede asociarse a una cuenta lista para cobro"
            );
        }

        return account;
    }

    private CustomerResponse findActiveCustomerByPhone(
            Long restaurantId,
            String phone) {

        List<?> results = entityManager
                .createNativeQuery("""
                        SELECT
                            id,
                            nombres,
                            apellidos,
                            telefono,
                            correo,
                            saldo_puntos,
                            total_visitas
                        FROM restaurante.clientes
                        WHERE restaurante_id = :restaurantId
                          AND telefono = :phone
                          AND activo = TRUE
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("phone", phone)
                .getResultList();

        if (results.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "customer_not_found",
                    "Cliente no encontrado",
                    "No existe un cliente activo con ese numero de telefono"
            );
        }

        return toCustomerResponse((Object[]) results.get(0));
    }

    private CustomerResponse findActiveCustomerById(
            Long restaurantId,
            Long customerId) {

        List<?> results = entityManager
                .createNativeQuery("""
                        SELECT
                            id,
                            nombres,
                            apellidos,
                            telefono,
                            correo,
                            saldo_puntos,
                            total_visitas
                        FROM restaurante.clientes
                        WHERE restaurante_id = :restaurantId
                          AND id = :customerId
                          AND activo = TRUE
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("customerId", customerId)
                .getResultList();

        if (results.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "customer_not_found",
                    "Cliente no encontrado",
                    "El cliente indicado no existe o no se encuentra activo"
            );
        }

        return toCustomerResponse((Object[]) results.get(0));
    }

    private boolean customerPhoneExists(
            Long restaurantId,
            String phone) {

        Number count = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM restaurante.clientes
                        WHERE restaurante_id = :restaurantId
                          AND telefono = :phone
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("phone", phone)
                .getSingleResult();

        return count.longValue() > 0;
    }

    private boolean customerEmailExists(
            Long restaurantId,
            String email) {

        Number count = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM restaurante.clientes
                        WHERE restaurante_id = :restaurantId
                          AND LOWER(BTRIM(correo)) = LOWER(BTRIM(:email))
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("email", email)
                .getSingleResult();

        return count.longValue() > 0;
    }

    private CustomerResponse toCustomerResponse(Object[] row) {
        return new CustomerResponse(
                ((Number) row[0]).longValue(),
                row[1].toString(),
                row[2] == null ? null : row[2].toString(),
                row[3].toString(),
                row[4] == null ? null : row[4].toString(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).intValue()
        );
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_customer_data",
                    "Datos de cliente invalidos",
                    "El valor indicado es obligatorio"
            );
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Long extractRestaurantId(Authentication authentication) {

        Object result = entityManager
                .createNativeQuery("""
                        SELECT u.restaurante_id
                        FROM restaurante.usuarios u
                        JOIN public.app_users au
                          ON au.id = u.id
                        WHERE au.email = :email
                        """)
                .setParameter("email", authentication.getName())
                .getSingleResult();

        return ((Number) result).longValue();
    }
}
