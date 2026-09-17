package com.restaurante.application.cash;

import com.restaurante.domain.model.CashTransaction;
import com.restaurante.domain.model.CashTransactionType;
import com.restaurante.domain.repository.CashTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class CashTransactionServiceTest {

    private CashTransactionRepository repository;
    private CashTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(CashTransactionRepository.class);
        service = new CashTransactionService(repository);
    }

    @Test
    void registraVentaDuranteTurnoAbierto() {
        when(repository.existsByPaymentId(20L)).thenReturn(false);

        service.registerSale(
                1L,
                10L,
                20L,
                4L,
                new BigDecimal("125.50")
        );

        ArgumentCaptor<CashTransaction> captor =
                ArgumentCaptor.forClass(CashTransaction.class);

        verify(repository).save(captor.capture());

        CashTransaction transaction = captor.getValue();

        assertEquals(1L, transaction.getCashShiftId());
        assertEquals(10L, transaction.getInvoiceId());
        assertEquals(20L, transaction.getPaymentId());
        assertEquals(CashTransactionType.VENTA, transaction.getType());
        assertEquals(new BigDecimal("125.50"), transaction.getAmount());
        assertEquals(0L, transaction.getPoints());
        assertEquals(4L, transaction.getRegisteredById());
    }

    @Test
    void noDuplicaVentaDelMismoPago() {
        when(repository.existsByPaymentId(20L)).thenReturn(true);

        service.registerSale(
                1L,
                10L,
                20L,
                4L,
                new BigDecimal("125.50")
        );

        verify(repository, never()).save(any());
    }

    @Test
    void registraPropina() {
        when(repository.existsByInvoiceIdAndType(
                10L,
                CashTransactionType.PROPINA
        )).thenReturn(false);

        service.registerTip(
                1L,
                10L,
                4L,
                new BigDecimal("15.00")
        );

        ArgumentCaptor<CashTransaction> captor =
                ArgumentCaptor.forClass(CashTransaction.class);

        verify(repository).save(captor.capture());

        CashTransaction transaction = captor.getValue();

        assertEquals(1L, transaction.getCashShiftId());
        assertEquals(10L, transaction.getInvoiceId());
        assertNull(transaction.getPaymentId());
        assertEquals(CashTransactionType.PROPINA, transaction.getType());
        assertEquals(new BigDecimal("15.00"), transaction.getAmount());
        assertEquals(0L, transaction.getPoints());
        assertEquals(4L, transaction.getRegisteredById());
    }

    @Test
    void noRegistraPropinaCero() {
        service.registerTip(
                1L,
                10L,
                4L,
                BigDecimal.ZERO
        );

        verify(repository, never()).save(any());
    }

    @Test
    void registraRedencionDePuntos() {
        when(repository.existsByInvoiceIdAndType(
                10L,
                CashTransactionType.REDENCION_PUNTOS
        )).thenReturn(false);

        service.registerPointsRedemption(
                1L,
                10L,
                4L,
                new BigDecimal("10.00"),
                100L
        );

        ArgumentCaptor<CashTransaction> captor =
                ArgumentCaptor.forClass(CashTransaction.class);

        verify(repository).save(captor.capture());

        CashTransaction transaction = captor.getValue();

        assertEquals(1L, transaction.getCashShiftId());
        assertEquals(10L, transaction.getInvoiceId());
        assertNull(transaction.getPaymentId());
        assertEquals(
                CashTransactionType.REDENCION_PUNTOS,
                transaction.getType()
        );
        assertEquals(new BigDecimal("10.00"), transaction.getAmount());
        assertEquals(100L, transaction.getPoints());
        assertEquals(4L, transaction.getRegisteredById());
    }

    @Test
    void noDuplicaRedencionDePuntos() {
        when(repository.existsByInvoiceIdAndType(
                10L,
                CashTransactionType.REDENCION_PUNTOS
        )).thenReturn(true);

        service.registerPointsRedemption(
                1L,
                10L,
                4L,
                new BigDecimal("10.00"),
                100L
        );

        verify(repository, never()).save(any());
    }
}