package com.illarilegal.pago;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

class CargoYapeServiceTest {
    private final PagoRepository repository = mock(PagoRepository.class);
    private final ResultadoPagoOutbox outbox = mock(ResultadoPagoOutbox.class);
    private final MercadoPagoYapeClient provider = mock(MercadoPagoYapeClient.class);
    private PagoEntity entity;
    private PagoService payments;
    private CargoYapeService service;

    @BeforeEach
    void setup() {
        entity =
                new PagoEntity(
                        14L, "buyer", new BigDecimal("1.00"), "PENDIENTE", Instant.now(), null);
        ReflectionTestUtils.setField(entity, "id", 14L);
        entity.prepararYape();
        when(repository.findById(14L)).thenReturn(Optional.of(entity));
        when(repository.findForUpdate(14L)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any(PagoEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(provider.modo()).thenReturn("live");
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        payments = new PagoService(repository, outbox);
        service = new CargoYapeService(repository, payments, outbox, provider, manager);
    }

    @Test
    void noReservaNiCobraUnPedidoPorDebajoDelMinimoConfigurado() {
        doThrow(
                        new ResponseStatusException(
                                HttpStatus.UNPROCESSABLE_ENTITY, "No alcanza el mínimo"))
                .when(provider)
                .verificarMonto(entity.getMonto());
        assertEquals(
                422,
                assertThrows(ResponseStatusException.class, this::charge).getStatusCode().value());
        assertNull(entity.getYapeIntento());
        verify(provider, never())
                .cobrar(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(outbox);
    }

    @Test
    void conservaElRechazoYBloqueaOtroCargoSinFingirUnPago() {
        when(provider.cobrar(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new MercadoPagoYapeClient.ImporteRechazado("referencia-proveedor"));
        assertEquals(
                422,
                assertThrows(ResponseStatusException.class, this::charge).getStatusCode().value());
        assertEquals("invalid_transaction_amount", entity.getYapeRechazoCodigo());
        assertNotNull(entity.getYapeIntento());
        assertEquals("PENDIENTE", entity.getEstado());
        assertNull(entity.getFechaAprobacion());
        assertFalse(payments.aPago(entity).yape().enVerificacion());
        assertEquals(
                422,
                assertThrows(ResponseStatusException.class, () -> service.checkout(14L, "buyer"))
                        .getStatusCode()
                        .value());
        assertEquals(
                422,
                assertThrows(ResponseStatusException.class, this::charge).getStatusCode().value());
        verify(provider, times(1))
                .cobrar(anyString(), anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(outbox);
    }

    @Test
    void unaConexionFallidaMantieneLaReservaHastaConciliar() {
        when(provider.cobrar(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(
                        new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY, "Resultado desconocido"));
        assertEquals(
                502,
                assertThrows(ResponseStatusException.class, this::charge).getStatusCode().value());
        assertNull(entity.getYapeRechazoCodigo());
        assertNotNull(entity.getYapeIntento());
        assertTrue(payments.aPago(entity).yape().enVerificacion());
        assertEquals(
                409,
                assertThrows(ResponseStatusException.class, () -> service.checkout(14L, "buyer"))
                        .getStatusCode()
                        .value());
        verifyNoInteractions(outbox);
    }

    @Test
    void conciliaElIntentoConfirmadoUnaSolaVezSinEmitirCargosNuevos() throws Exception {
        entity.reservarCargoYape("attempt", "live", "hash");
        when(provider.buscarOrden(eq("attempt"), any())).thenReturn(Optional.of("ORD1234567890"));
        when(provider.esOrdenDelEntorno("ORD1234567890")).thenReturn(true);
        when(provider.consultar("ORD1234567890")).thenReturn(new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
          {"id":"ORD1234567890","external_reference":"attempt","type":"online","processing_mode":"automatic",
          "currency":"PEN","total_amount":"1.00","total_paid_amount":"1.00","status":"processed","status_detail":"accredited",
          "transactions":{"payments":[{"id":"pay","amount":"1.00","paid_amount":"1.00","status":"processed",
          "status_detail":"accredited","payment_method":{"id":"yape"}}]}}
          """));
        service.revisarPendiente(14L); service.revisarPendiente(14L);
        assertEquals("VALIDADO", entity.getEstado()); verify(outbox, times(1)).registrar(any());
        verify(provider, never()).cobrar(anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test
    void laBusquedaVaciaNuncaLiberaElIntentoNiLoDeclaraPagado() {
        entity.reservarCargoYape("attempt", "live", "hash");
        when(provider.buscarOrden(eq("attempt"),any())).thenReturn(Optional.empty());
        service.revisarPendiente(14L);
        assertEquals("PENDIENTE",entity.getEstado()); assertEquals("attempt",entity.getYapeIntento());
        verifyNoInteractions(outbox);
        verify(provider,never()).cobrar(anyString(),anyString(),anyString(),anyString(),anyString());
    }
    @Test
    void noConsultaIntentosRechazadosNiDeOtroEntorno() {
        entity.reservarCargoYape("attempt", "test", "hash"); service.revisarPendiente(14L);
        entity.reservarCargoYape("attempt", "live", "hash"); entity.registrarRechazoImporte("provider"); service.revisarPendiente(14L);
        verify(provider,never()).buscarOrden(anyString(),any()); verifyNoInteractions(outbox);
    }

    private void charge() {
        service.cobrar(
                14L,
                "buyer",
                "buyer@example.com",
                "0123456789abcdef0123456789abcdef",
                "999999999",
                null);
    }
}
