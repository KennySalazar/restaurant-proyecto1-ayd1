package com.restaurante.application.payment;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.payment.CustomerPointsResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoyaltyService {

    private final EntityManager entityManager;

    public LoyaltyService(EntityManager entityManager) {
        this.entityManager = entityManager;
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