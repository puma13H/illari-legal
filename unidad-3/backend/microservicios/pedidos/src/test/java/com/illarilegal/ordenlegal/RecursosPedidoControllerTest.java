package com.illarilegal.ordenlegal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

class RecursosPedidoControllerTest {
    private Jwt token(String subject) {
        return Jwt.withTokenValue("unit")
                .header("alg", "RS256")
                .subject(subject)
                .claim("realm_access", Map.of("roles", List.of("CLIENTE")))
                .build();
    }

    private RecursosPedidoController controller(String state, String details) {
        var repository = mock(OrdenRepository.class);
        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(
                                new OrdenEntity(
                                        "cliente-a",
                                        Instant.now(),
                                        state,
                                        BigDecimal.ONE,
                                        "YAPE",
                                        details)));
        return new RecursosPedidoController(repository, new ObjectMapper(), 15);
    }

    @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory;
    @Test
    void entregaLaVersionCompradaYNoUnaRutaLibreNiOtroServicio() throws Exception {
        String version = java.util.UUID.randomUUID().toString();
        java.nio.file.Files.writeString(directory.resolve(version + ".pdf"), "%PDF-original");
        var repository = mock(OrdenRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(new OrdenEntity("cliente-a", Instant.now(), "PAGADA", BigDecimal.ONE, "YAPE",
                "[{\"idServicio\":30,\"nombreServicio\":\"Documento\",\"tipoEntrega\":\"ARCHIVO\",\"archivoVersion\":\""+version+"\"}]")));
        var controller = new RecursosPedidoController(repository, new ObjectMapper(), 15, directory.toString());
        assertTrue(controller.recursos(1L, token("cliente-a")).getFirst().disponible());
        assertEquals("%PDF-original", new String(controller.descargarRecurso(1L,30L,token("cliente-a")).getBody().getInputStream().readAllBytes()));
        assertThrows(ResponseStatusException.class, () -> controller.descargarRecurso(1L,31L,token("cliente-a")));
        java.nio.file.Files.delete(directory.resolve(version + ".pdf"));
        assertFalse(controller.recursos(1L, token("cliente-a")).getFirst().disponible());
        assertThrows(ResponseStatusException.class, () -> controller.descargarRecurso(1L,30L,token("cliente-a")));
    }

    @Test
    void noEntregaRecursosDeOtroCliente() {
        var controller = controller("PAGADA", "[{\"idServicio\":15}]");
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () -> controller.recursos(1L, token("cliente-b")))
                        .getStatusCode()
                        .value());
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () -> controller.descargar(1L, token("cliente-b")))
                        .getStatusCode()
                        .value());
    }

    @Test
    void noEntregaGuiaAntesDeConfirmarElPago() {
        var controller = controller("PENDIENTE_PAGO", "[{\"idServicio\":15}]");
        assertTrue(controller.recursos(1L, token("cliente-a")).isEmpty());
        assertEquals(
                409,
                assertThrows(
                                ResponseStatusException.class,
                                () -> controller.descargar(1L, token("cliente-a")))
                        .getStatusCode()
                        .value());
    }

    @Test
    void unPagoDeOtroServicioNoHabilitaLaGuia() {
        var controller = controller("PAGADA", "[{\"idServicio\":2}]");
        assertTrue(controller.recursos(1L, token("cliente-a")).isEmpty());
        assertEquals(
                404,
                assertThrows(
                                ResponseStatusException.class,
                                () -> controller.descargar(1L, token("cliente-a")))
                        .getStatusCode()
                        .value());
    }

    @Test
    void entregaElPdfAlTitularDelPedidoPagado() throws Exception {
        var controller = controller("PAGADA", "[{\"idServicio\":2},{\"idServicio\":15}]");
        assertEquals(
                "guia-bienvenida", controller.recursos(1L, token("cliente-a")).getFirst().codigo());
        var response = controller.descargar(1L, token("cliente-a"));
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertArrayEquals("%PDF-".getBytes(), response.getBody().getInputStream().readNBytes(5));
    }
}
