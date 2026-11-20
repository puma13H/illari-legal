package com.casoslegales.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.casoslegales.model.*;
import com.casoslegales.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class SolicitudesServiceTest {
    final PedidosContratadosClient orders = mock(PedidosContratadosClient.class);
    final IdentidadKeycloakRepository identities = mock(IdentidadKeycloakRepository.class);
    final ClienteRepository clients = mock(ClienteRepository.class);
    final AbogadoRepository lawyers = mock(AbogadoRepository.class);
    final CasoRepository cases = mock(CasoRepository.class);
    final CasoService caseService = mock(CasoService.class);
    final Cliente buyer = mock(Cliente.class);
    final Abogado lawyer = mock(Abogado.class);
    final ObjectMapper json = new ObjectMapper();
    final SolicitudesService service = new SolicitudesService(orders, identities, clients, lawyers, cases, caseService, "authority");

    @BeforeEach void setup() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(3L);
        when(identities.findByIssuerAndSubject("authority", "buyer-sub"))
                .thenReturn(Optional.of(new IdentidadKeycloak("authority", "buyer-sub", user, "operator")));
        when(clients.findByUserId(3L)).thenReturn(Optional.of(buyer));
        when(buyer.getId()).thenReturn(4L);
        when(clients.findForUpdate(4L)).thenReturn(Optional.of(buyer));
        when(lawyers.findById(5L)).thenReturn(Optional.of(lawyer));
        when(lawyer.getId()).thenReturn(5L);
        when(cases.saveAndFlush(any())).thenAnswer(i -> {
            Caso c = i.getArgument(0); ReflectionTestUtils.setField(c, "id", 7L); return c;
        });
    }
    private void order(String status, String delivery) throws Exception {
        when(orders.pedido(1, "jwt")).thenReturn(json.readTree("""
            {"id":1,"idCliente":"buyer-sub","estado":"%s","detalles":[
            {"nombreServicio":"Asesoría civil","tipoEntrega":"%s","subtotal":80,"cantidad":1}]}
            """.formatted(status, delivery)));
    }
    @Test void creaUnExpedienteDelCompradorAutenticadoYDelImporteContratado() throws Exception {
        order("PAGADA", "ASESORIA");
        service.asignar(1, 0, 5L, "Indicaciones", "jwt", "admin@example.invalid");
        var argument = org.mockito.ArgumentCaptor.forClass(Caso.class);
        verify(cases).saveAndFlush(argument.capture());
        Caso c = argument.getValue();
        assertSame(buyer, c.getCliente()); assertSame(lawyer, c.getAbogado());
        assertEquals(1L, c.getPedidoId()); assertEquals("Asesoría civil", c.getServicioContratado());
        assertEquals(0, c.getTarifa().compareTo(new java.math.BigDecimal("80")));
    }
    @Test void rechazaPedidosSinPagoYProductosQueNoSonAsesorias() throws Exception {
        order("PENDIENTE_PAGO", "ASESORIA");
        assertThrows(ResponseStatusException.class, () -> service.asignar(1,0,5L,"","jwt","admin"));
        order("PAGADA", "ARCHIVO");
        assertThrows(ResponseStatusException.class, () -> service.asignar(1,0,5L,"","jwt","admin"));
        verify(cases, never()).saveAndFlush(any());
    }
    @Test void noPermiteAsignarComprasDeUnUsuarioSinVinculo() throws Exception {
        order("PAGADA", "ASESORIA");
        when(identities.findByIssuerAndSubject(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.asignar(1,0,5L,"","jwt","admin"));
        verify(cases, never()).saveAndFlush(any());
    }
    @Test void unaAsignacionRepetidaNoCreaOtroExpedienteNiReasignaEnSilencio() throws Exception {
        order("PAGADA", "ASESORIA");
        Caso existing = new Caso(); existing.setAbogado(lawyer);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(cases.findByPedidoIdAndPedidoDetalle(1L,0)).thenReturn(Optional.of(existing));
        service.asignar(1,0,5L,"","jwt","admin");
        verify(caseService).getById(7L,"admin","admin");
        assertThrows(ResponseStatusException.class, () -> service.asignar(1,0,6L,"","jwt","admin"));
        verify(cases, never()).saveAndFlush(any());
    }
    @Test void losPedidosHistoricosSinPerfilNoOcultanLasDemasSolicitudes() throws Exception {
        when(orders.pagados(0,"jwt")).thenReturn(json.readTree("""
          {"items":[{"id":2,"idCliente":"retired","estado":"PAGADA","detalles":[
          {"tipoEntrega":"ASESORIA","nombreServicio":"Consulta","subtotal":80,"cantidad":1}]}],"hayMas":false}
          """));
        assertNull(service.listar(0,"jwt").items().getFirst().clienteId());
        verify(cases, never()).saveAndFlush(any());
    }
}
