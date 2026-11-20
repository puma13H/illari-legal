package com.illarilegal.pago;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class MercadoPagoYapeClientTest {
    private MercadoPagoYapeClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        client =
                new MercadoPagoYapeClient(
                        RestClient.builder(),
                        new ObjectMapper(),
                        true,
                        "live",
                        "APP_USR-00000000-0000-0000-0000-000000000000",
                        "APP_USR-local-unit-test",
                        123L,
                        100L);
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "api", builder.build());
    }

    @Test
    void buscaSoloPorGetYReferenciaExacta() {
        server.expect(anything()).andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("external_reference", "attempt"))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"ORD1234567890\",\"external_reference\":\"attempt\"}],\"paging\":{\"total\":1}}", MediaType.APPLICATION_JSON));
        assertEquals("ORD1234567890", client.buscarOrden("attempt", java.time.Instant.now()).orElseThrow());
        server.verify();
    }
    @Test
    void buscarSinResultadosMantieneElResultadoPendiente() {
        server.expect(anything()).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"data\":[],\"paging\":{\"total\":0}}", MediaType.APPLICATION_JSON));
        assertTrue(client.buscarOrden("attempt", java.time.Instant.now()).isEmpty()); server.verify();
    }
    @Test
    void noAceptaReferenciasDistintasNiResultadosAmbiguos() {
        server.expect(anything()).andRespond(withSuccess("{\"data\":[{\"id\":\"ORD1234567890\",\"external_reference\":\"other\"}],\"paging\":{\"total\":1}}", MediaType.APPLICATION_JSON));
        assertThrows(ResponseStatusException.class, () -> client.buscarOrden("attempt", java.time.Instant.now())); server.verify();
    }
    @Test
    void respetaLaEsperaDelProveedorSinReintentarUnCargo() {
        server.expect(anything()).andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "7200"));
        assertThrows(ResponseStatusException.class, () -> client.buscarOrden("attempt", java.time.Instant.now()));
        assertFalse(client.permiteConsulta());
        assertThrows(ResponseStatusException.class, () -> client.buscarOrden("attempt", java.time.Instant.now())); server.verify();
    }

    @Test
    void validaElMinimoLocalSinEnviarUnaSolicitudAlProveedor() {
        ReflectionTestUtils.setField(client, "minimoCentimos", 124L);
        assertEquals(
                422,
                assertThrows(
                                ResponseStatusException.class,
                                () -> client.verificarMonto(new java.math.BigDecimal("1.00")))
                        .getStatusCode()
                        .value());
        assertDoesNotThrow(() -> client.verificarMonto(new java.math.BigDecimal("1.24")));
        server.verify();
    }

    @Test
    void identificaSoloElRechazoExplicitoDelImporte() {
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Idempotency-Key", "intento-local"))
                .andExpect(
                        content()
                                .json(
                                        """
                                        {"total_amount":"1.00","transactions":{"payments":[{"amount":"1.00","payment_method":{"id":"yape","type":"debit_card"}}]}}
                                        """))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("x-request-id", "referencia-del-proveedor")
                                .body(
                                        "{\"errors\":[{\"code\":\"invalid_transaction_amount\",\"message\":\"Invalid"
                                            + " value for transaction_amount.\"}]}"));
        var error = assertThrows(MercadoPagoYapeClient.ImporteRechazado.class, this::send);
        assertEquals("referencia-del-proveedor", error.requestId());
        assertFalse(error.getMessage().contains("private-test-token"));
        server.verify();
    }

    @Test
    void unErrorDelServidorSigueSiendoUnResultadoDesconocido() {
        server.expect(anything())
                .andRespond(
                        withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"errors\":[{\"code\":\"invalid_transaction_amount\"}]}"));
        var error = assertThrows(ResponseStatusException.class, this::send);
        assertEquals(502, error.getStatusCode().value());
        server.verify();
    }

    @Test
    void noConfundeUnaRespuestaIncompletaConUnRechazoSeguro() {
        server.expect(anything())
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("not-json"));
        assertEquals(
                502,
                assertThrows(ResponseStatusException.class, this::send).getStatusCode().value());
        server.verify();
    }

    @Test
    void noDeclaraRechazoSeguroSiLaRespuestaIncluyeUnaOrden() {
        server.expect(anything())
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        "{\"id\":\"ORD123\",\"errors\":[{\"code\":\"invalid_transaction_amount\"}]}"));
        assertEquals(
                502,
                assertThrows(ResponseStatusException.class, this::send).getStatusCode().value());
        server.verify();
    }

    private void send() {
        client.cobrar(
                "private-test-token", "buyer@example.com", "999999999", "1.00", "intento-local");
    }
}
