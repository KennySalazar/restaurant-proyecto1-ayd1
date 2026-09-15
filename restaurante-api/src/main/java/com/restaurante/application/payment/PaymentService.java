package com.restaurante.application.payment;

import com.restaurante.application.billing.BillingCalculationService;
import com.restaurante.application.cash.CashShiftService;
import com.restaurante.application.cash.CashTransactionService;
import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.CashShift;
import com.restaurante.domain.model.Payment;
import com.restaurante.domain.model.PaymentMethod;
import com.restaurante.domain.model.RestaurantConfiguration;
import com.restaurante.domain.model.RestaurantConfigurationStatus;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.PaymentMethodRepository;
import com.restaurante.domain.repository.PaymentRepository;
import com.restaurante.domain.repository.RestaurantConfigurationRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.billing.BillingCalculationResponse;
import com.restaurante.web.dto.payment.ChargeRequest;
import com.restaurante.web.dto.payment.ChargeResponse;
import com.restaurante.web.dto.payment.PaymentResponse;
import com.restaurante.web.dto.payment.RegisterPaymentRequest;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    private static final String DOCUMENT_TYPE = "COMPROBANTE";

    private final AccountRepository accounts;
    private final PaymentRepository payments;
    private final PaymentMethodRepository paymentMethods;
    private final RestaurantConfigurationRepository configurations;
    private final BillingCalculationService billingCalculationService;
    private final CashShiftService cashShiftService;
    private final CashTransactionService cashTransactionService;
    private final EntityManager entityManager;

    public PaymentService(
            AccountRepository accounts,
            PaymentRepository payments,
            PaymentMethodRepository paymentMethods,
            RestaurantConfigurationRepository configurations,
            BillingCalculationService billingCalculationService,
            CashShiftService cashShiftService,
            CashTransactionService cashTransactionService,
            EntityManager entityManager) {

        this.accounts = accounts;
        this.payments = payments;
        this.paymentMethods = paymentMethods;
        this.configurations = configurations;
        this.billingCalculationService = billingCalculationService;
        this.cashShiftService = cashShiftService;
        this.cashTransactionService = cashTransactionService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ChargeResponse chargeAccount(
            Long accountId,
            ChargeRequest request,
            Authentication authentication) {

        CashShift shift =
                cashShiftService.requireOpenShift(authentication);

        BillingCalculationResponse calculation =
                billingCalculationService.calculateAccount(
                        accountId,
                        authentication
                );

        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta indicada no existe"
                ));

        if (hasActiveSubaccounts(accountId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_has_active_subaccounts",
                    "Cuenta dividida",
                    "La cuenta tiene subcuentas activas y debe cobrarse por subcuentas"
            );
        }

        RestaurantConfiguration configuration =
                configurations
                        .findByRestaurantIdAndStatus(
                                account.getRestaurantId(),
                                RestaurantConfigurationStatus.VIGENTE
                        )
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "restaurant_configuration_not_found",
                                "Configuracion no encontrada",
                                "El restaurante no tiene una configuracion vigente"
                        ));

        BigDecimal invoiceTotal = money(calculation.total());

        if (invoiceTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "empty_account_total",
                    "Cuenta sin monto cobrable",
                    "La cuenta no posee un total mayor que cero para cobrar"
            );
        }

        List<ValidatedPayment> validatedPayments =
                validatePayments(
                        request.pagos(),
                        invoiceTotal
                );

        String series =
                findActiveSeries(account.getRestaurantId());

        Long sequence =
                nextInvoiceNumber(
                        account.getRestaurantId(),
                        series
                );

        String documentNumber =
                series + "-" + sequence;

        long pointsGranted = 0L;

        if (account.getClientId() != null) {
            pointsGranted = invoiceTotal
                    .multiply(configuration.getPointsPerCurrency())
                    .setScale(0, RoundingMode.FLOOR)
                    .longValue();
        }

        Long invoiceId = insertInvoice(
                account,
                shift,
                configuration,
                calculation,
                series,
                sequence,
                documentNumber,
                pointsGranted
        );

        insertInvoiceDetails(
                invoiceId,
                accountId
        );

        insertInvoiceModifiers(invoiceId);

        List<PaymentResult> registeredPayments =
                new ArrayList<>();

        for (ValidatedPayment validated : validatedPayments) {

            RegisterPaymentRequest paymentRequest =
                    validated.request();

            PaymentMethod method =
                    validated.method();

            Payment payment = new Payment(
                    invoiceId,
                    method.getId(),
                    money(paymentRequest.monto()),
                    paymentRequest.montoRecibido() == null
                            ? null
                            : money(paymentRequest.montoRecibido()),
                    paymentRequest.referencia(),
                    paymentRequest.autorizacion(),
                    shift.getCashierId()
            );

            payment = payments.saveAndFlush(payment);

            entityManager.refresh(payment);

            cashTransactionService.registerSale(
                    shift.getId(),
                    invoiceId,
                    payment.getId(),
                    shift.getCashierId(),
                    payment.getAmount()
            );

            registeredPayments.add(
                    new PaymentResult(
                            payment,
                            method.getCode()
                    )
            );
        }

        cashTransactionService.registerTip(
                shift.getId(),
                invoiceId,
                shift.getCashierId(),
                calculation.montoPropina()
        );

        entityManager.flush();

        account.setStatus("CERRADA");
        accounts.saveAndFlush(account);

        BigDecimal totalPaid = money(
                registeredPayments
                        .stream()
                        .map(result -> result.payment().getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        BigDecimal pending =
                money(invoiceTotal.subtract(totalPaid));

        List<PaymentResponse> paymentResponses =
                registeredPayments
                        .stream()
                        .map(result -> new PaymentResponse(
                                result.payment().getId(),
                                invoiceId,
                                account.getId(),
                                null,
                                result.methodCode(),
                                result.payment().getAmount(),
                                result.payment().getReceivedAmount(),
                                result.payment().getChangeGiven(),
                                result.payment().getReference(),
                                result.payment().getAuthorization(),
                                invoiceTotal,
                                totalPaid,
                                pending,
                                account.getStatus(),
                                result.payment().getPaidAt()
                        ))
                        .toList();

        return new ChargeResponse(
                invoiceId,
                documentNumber,
                account.getId(),
                invoiceTotal,
                totalPaid,
                pending,
                account.getStatus(),
                paymentResponses
        );
    }

    private List<ValidatedPayment> validatePayments(
            List<RegisterPaymentRequest> requests,
            BigDecimal invoiceTotal) {

        List<ValidatedPayment> validated =
                new ArrayList<>();

        BigDecimal paymentSum =
                BigDecimal.ZERO;

        for (RegisterPaymentRequest request : requests) {

            PaymentMethod method = paymentMethods
                    .findByIdAndActiveTrue(request.metodoPagoId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "payment_method_not_found",
                            "Metodo de pago no encontrado",
                            "El metodo de pago no existe o se encuentra inactivo"
                    ));

            BigDecimal amount =
                    money(request.monto());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_payment_amount",
                        "Monto invalido",
                        "Todos los pagos deben tener un monto mayor que cero"
                );
            }

            if (method.isRequiresReference()
                    && (request.referencia() == null
                    || request.referencia().isBlank())) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "payment_reference_required",
                        "Referencia requerida",
                        "El metodo de pago "
                                + method.getCode()
                                + " requiere una referencia"
                );
            }

            if (!method.isAffectsCash()
                    && request.montoRecibido() != null) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "received_amount_not_allowed",
                        "Monto recibido no permitido",
                        "El monto recibido solamente aplica a pagos en efectivo"
                );
            }

            if (method.isAffectsCash()
                    && request.montoRecibido() != null
                    && money(request.montoRecibido())
                    .compareTo(amount) < 0) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "insufficient_received_amount",
                        "Efectivo insuficiente",
                        "El monto recibido no puede ser menor que el monto aplicado"
                );
            }

            paymentSum =
                    paymentSum.add(amount);

            validated.add(
                    new ValidatedPayment(
                            request,
                            method
                    )
            );
        }

        paymentSum = money(paymentSum);

        int comparison =
                paymentSum.compareTo(invoiceTotal);

        if (comparison < 0) {

            BigDecimal pending =
                    money(invoiceTotal.subtract(paymentSum));

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "insufficient_payment_total",
                    "Pago incompleto",
                    "Los pagos no cubren el total de la cuenta. Monto pendiente: Q"
                            + pending.toPlainString()
            );
        }

        if (comparison > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "payment_total_exceeded",
                    "Pago excedido",
                    "La suma de los pagos supera el total de la cuenta"
            );
        }

        return validated;
    }

    private boolean hasActiveSubaccounts(Long accountId) {

        Number count = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM restaurante.subcuentas
                        WHERE cuenta_id = :accountId
                          AND estado <> 'CANCELADA'
                        """)
                .setParameter("accountId", accountId)
                .getSingleResult();

        return count.longValue() > 0;
    }

    private String findActiveSeries(Long restaurantId) {

        List<?> results = entityManager
                .createNativeQuery("""
                    SELECT serie
                    FROM restaurante.secuencias_facturacion
                    WHERE restaurante_id = :restaurantId
                      AND tipo_documento = :documentType
                      AND activo = TRUE
                    ORDER BY id
                    LIMIT 1
                    """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("documentType", DOCUMENT_TYPE)
                .getResultList();

        if (results.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invoice_sequence_not_found",
                    "Secuencia de facturacion no encontrada",
                    "No existe una secuencia activa para emitir el comprobante"
            );
        }

        return results.getFirst().toString();
    }

    private Long nextInvoiceNumber(
            Long restaurantId,
            String series) {

        Number result = (Number) entityManager
                .createNativeQuery("""
                        SELECT restaurante.fn_siguiente_numero_factura(
                            :restaurantId,
                            :documentType,
                            :series
                        )
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("documentType", DOCUMENT_TYPE)
                .setParameter("series", series)
                .getSingleResult();

        return result.longValue();
    }

    private Long insertInvoice(
            Account account,
            CashShift shift,
            RestaurantConfiguration configuration,
            BillingCalculationResponse calculation,
            String series,
            Long sequence,
            String documentNumber,
            long pointsGranted) {

        Number result = (Number) entityManager
                .createNativeQuery("""
                        INSERT INTO restaurante.facturas (
                            restaurante_id,
                            cuenta_id,
                            subcuenta_id,
                            turno_caja_id,
                            configuracion_id,
                            cliente_id,
                            mesero_id,
                            cajero_id,
                            tipo_documento,
                            serie,
                            numero_correlativo,
                            numero_documento,
                            estado,
                            subtotal,
                            descuento_total,
                            descuento_puntos,
                            porcentaje_impuesto,
                            monto_impuesto,
                            porcentaje_propina,
                            monto_propina,
                            total,
                            puntos_redimidos,
                            puntos_otorgados
                        )
                        VALUES (
                            :restaurantId,
                            :accountId,
                            NULL,
                            :shiftId,
                            :configurationId,
                            :clientId,
                            :waiterId,
                            :cashierId,
                            :documentType,
                            :series,
                            :sequence,
                            :documentNumber,
                            'EMITIDA',
                            :subtotal,
                            0,
                            0,
                            :taxPercentage,
                            :taxAmount,
                            :tipPercentage,
                            :tipAmount,
                            :total,
                            0,
                            :pointsGranted
                        )
                        RETURNING id
                        """)
                .setParameter(
                        "restaurantId",
                        account.getRestaurantId()
                )
                .setParameter(
                        "accountId",
                        account.getId()
                )
                .setParameter(
                        "shiftId",
                        shift.getId()
                )
                .setParameter(
                        "configurationId",
                        configuration.getId()
                )
                .setParameter(
                        "clientId",
                        account.getClientId()
                )
                .setParameter(
                        "waiterId",
                        account.getWaiterId()
                )
                .setParameter(
                        "cashierId",
                        shift.getCashierId()
                )
                .setParameter(
                        "documentType",
                        DOCUMENT_TYPE
                )
                .setParameter(
                        "series",
                        series
                )
                .setParameter(
                        "sequence",
                        sequence
                )
                .setParameter(
                        "documentNumber",
                        documentNumber
                )
                .setParameter(
                        "subtotal",
                        calculation.subtotal()
                )
                .setParameter(
                        "taxPercentage",
                        calculation.porcentajeImpuesto()
                )
                .setParameter(
                        "taxAmount",
                        calculation.montoImpuesto()
                )
                .setParameter(
                        "tipPercentage",
                        calculation.porcentajePropina()
                )
                .setParameter(
                        "tipAmount",
                        calculation.montoPropina()
                )
                .setParameter(
                        "total",
                        calculation.total()
                )
                .setParameter(
                        "pointsGranted",
                        pointsGranted
                )
                .getSingleResult();

        return result.longValue();
    }

    private void insertInvoiceDetails(
            Long invoiceId,
            Long accountId) {

        entityManager
                .createNativeQuery("""
                        INSERT INTO restaurante.factura_detalles (
                            factura_id,
                            comanda_detalle_id,
                            platillo_id,
                            combo_id,
                            nombre_snapshot,
                            categoria_snapshot,
                            cantidad,
                            precio_unitario_snapshot,
                            total_modificadores_snapshot,
                            costo_unitario_snapshot,
                            subtotal_linea
                        )
                        SELECT
                            :invoiceId,
                            cd.id,
                            cd.platillo_id,
                            cd.combo_id,
                            cd.nombre_snapshot,
                            NULL,
                            cd.cantidad,
                            cd.precio_unitario_snapshot,
                            ROUND(
                                COALESCE((
                                    SELECT SUM(
                                        cdm.cantidad
                                        * cdm.precio_adicional_snapshot
                                    )
                                    FROM restaurante.comanda_detalle_modificadores cdm
                                    WHERE cdm.comanda_detalle_id = cd.id
                                ), 0),
                                2
                            ),
                            ROUND(
                                GREATEST(
                                    cd.costo_unitario_snapshot
                                    + COALESCE((
                                        SELECT SUM(
                                            cdm.cantidad
                                            * cdm.costo_adicional_snapshot
                                        )
                                        FROM restaurante.comanda_detalle_modificadores cdm
                                        WHERE cdm.comanda_detalle_id = cd.id
                                    ), 0),
                                    0
                                ),
                                4
                            ),
                            ROUND(
                                cd.cantidad * (
                                    cd.precio_unitario_snapshot
                                    + COALESCE((
                                        SELECT SUM(
                                            cdm.cantidad
                                            * cdm.precio_adicional_snapshot
                                        )
                                        FROM restaurante.comanda_detalle_modificadores cdm
                                        WHERE cdm.comanda_detalle_id = cd.id
                                    ), 0)
                                ),
                                2
                            )
                        FROM restaurante.comanda_detalles cd
                        JOIN restaurante.comandas co
                          ON co.id = cd.comanda_id
                        WHERE co.cuenta_id = :accountId
                          AND cd.estado = 'ENTREGADO'
                        """)
                .setParameter(
                        "invoiceId",
                        invoiceId
                )
                .setParameter(
                        "accountId",
                        accountId
                )
                .executeUpdate();
    }

    private void insertInvoiceModifiers(Long invoiceId) {

        entityManager
                .createNativeQuery("""
                        INSERT INTO restaurante.factura_detalle_modificadores (
                            factura_detalle_id,
                            modificador_id,
                            nombre_snapshot,
                            cantidad,
                            precio_adicional_snapshot
                        )
                        SELECT
                            fd.id,
                            cdm.modificador_id,
                            cdm.nombre_snapshot,
                            cdm.cantidad,
                            cdm.precio_adicional_snapshot
                        FROM restaurante.factura_detalles fd
                        JOIN restaurante.comanda_detalle_modificadores cdm
                          ON cdm.comanda_detalle_id =
                             fd.comanda_detalle_id
                        WHERE fd.factura_id = :invoiceId
                        """)
                .setParameter(
                        "invoiceId",
                        invoiceId
                )
                .executeUpdate();
    }

    private BigDecimal money(BigDecimal value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private record ValidatedPayment(
            RegisterPaymentRequest request,
            PaymentMethod method) {
    }

    private record PaymentResult(
            Payment payment,
            String methodCode) {
    }
}