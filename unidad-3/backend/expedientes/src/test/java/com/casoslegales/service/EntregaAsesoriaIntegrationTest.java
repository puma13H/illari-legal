package com.casoslegales.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.casoslegales.model.*;
import com.casoslegales.repository.CasoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.access.AccessDeniedException;

/** Persiste únicamente en H2; no altera pedidos, cuentas ni pagos de la instalación. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false", "spring.jpa.show-sql=false", "ILLARI_IDENTITY_AUTHORITY=authority"}, showSql=false)
@ActiveProfiles("integration")
@Import({SolicitudesService.class, CasoService.class})
class EntregaAsesoriaIntegrationTest {
    @Autowired TestEntityManager database;
    @Autowired SolicitudesService requests;
    @Autowired CasoService cases;
    @Autowired CasoRepository repository;
    @MockitoBean PedidosContratadosClient orders;

    private User user(String email, Role role) {
        var user = new User(); user.setName(email); user.setEmail(email); user.setPassword("unused-identity-provider"); user.setRole(role);
        return database.persist(user);
    }
    private Cliente client(String email, Role role) {
        var c = new Cliente(); c.setNombre(email); c.setTelefono("000000000"); c.setEmail(email); c.setUser(user(email,role));
        return database.persist(c);
    }
    @Test void guardaLaAtencionUnaVezYLaHaceVisibleSoloASusResponsables() throws Exception {
        var clientRole = new Role(); clientRole.setName("cliente"); database.persist(clientRole);
        var lawyerRole = new Role(); lawyerRole.setName("abogado"); database.persist(lawyerRole);
        var buyer = client("buyer@example.invalid",clientRole);
        client("other@example.invalid",clientRole);
        var lawyer = new Abogado(); lawyer.setNombre("Abogado asignado"); lawyer.setEmail("lawyer@example.invalid");
        lawyer.setTelefono("000000000"); lawyer.setEspecialidad("Civil"); lawyer.setUser(user(lawyer.getEmail(),lawyerRole)); database.persist(lawyer);
        database.persist(new IdentidadKeycloak("authority","buyer-sub",buyer.getUser(),"operator")); database.flush();
        when(orders.pedido(40,"jwt")).thenReturn(new ObjectMapper().readTree("""
          {"id":40,"idCliente":"buyer-sub","estado":"PAGADA","detalles":[{"tipoEntrega":"ASESORIA",
          "nombreServicio":"Asesoría civil","subtotal":80,"cantidad":1}]}
          """));
        var created = requests.asignar(40,0,lawyer.getId(),"Atención contratada","jwt","admin");
        var repeated = requests.asignar(40,0,lawyer.getId(),"","jwt","admin");
        database.flush(); database.clear();
        assertEquals(created.id(),repeated.id()); assertEquals(1,repository.count());
        assertEquals(40L,cases.getById(created.id(),"buyer@example.invalid","cliente").pedidoId());
        assertEquals(created.id(),cases.getById(created.id(),"lawyer@example.invalid","abogado").id());
        assertThrows(AccessDeniedException.class, () -> cases.getById(created.id(),"other@example.invalid","cliente"));
        assertEquals("Asesoría civil",repository.findByPedidoIdAndPedidoDetalle(40L,0).orElseThrow().getServicioContratado());
    }
}
