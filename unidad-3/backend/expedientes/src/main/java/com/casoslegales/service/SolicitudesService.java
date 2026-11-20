package com.casoslegales.service;

import com.casoslegales.dto.CasoResponse;
import com.casoslegales.model.*;
import com.casoslegales.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class SolicitudesService {
    private final PedidosContratadosClient pedidos;
    private final IdentidadKeycloakRepository identities;
    private final ClienteRepository clients;
    private final AbogadoRepository lawyers;
    private final CasoRepository cases;
    private final CasoService caseService;
    private final String authority;

    public SolicitudesService(PedidosContratadosClient pedidos, IdentidadKeycloakRepository identities,
            ClienteRepository clients, AbogadoRepository lawyers, CasoRepository cases, CasoService caseService,
            @Value("${ILLARI_IDENTITY_AUTHORITY:${spring.security.oauth2.resourceserver.jwt.issuer-uri}}") String authority) {
        this.pedidos=pedidos; this.identities=identities; this.clients=clients; this.lawyers=lawyers;
        this.cases=cases; this.caseService=caseService; this.authority=authority;
    }

    @Transactional(readOnly=true)
    public Pagina listar(int page, String token) {
        if (page < 0 || page > 100000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        JsonNode result = pedidos.pagados(page, token);
        if (result == null || !result.path("items").isArray()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        var items = new ArrayList<Solicitud>();
        for (JsonNode order : result.path("items")) {
            if (!"PAGADA".equals(order.path("estado").asText())) continue;
            var person = buscarCliente(order.path("idCliente").asText());
            JsonNode details=order.path("detalles");
            for (int position=0; position<details.size(); position++) {
                JsonNode detail=details.get(position);
                if (!"ASESORIA".equals(detail.path("tipoEntrega").asText())) continue;
                var linked=cases.findByPedidoIdAndPedidoDetalle(order.path("id").asLong(), position);
                items.add(new Solicitud(order.path("id").asLong(), position, detail.path("nombreServicio").asText(),
                        person.map(Cliente::getNombre).orElse("Comprador sin perfil vinculado"), person.map(Cliente::getId).orElse(null), detail.path("subtotal").decimalValue(),
                        detail.path("cantidad").asInt(), order.path("fechaCreacion").asText(),
                        linked.map(Caso::getId).orElse(null),
                        linked.map(c -> c.getAbogado().getNombre()).orElse(null)));
            }
        }
        return new Pagina(items, page, result.path("hayMas").asBoolean());
    }

    @Transactional
    public CasoResponse asignar(long orderId, int position, Long lawyerId, String description, String token,
            String operatorEmail) {
        if (orderId < 1 || position < 0 || lawyerId == null || description != null && description.length()>2000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa la solicitud y el abogado.");
        JsonNode order=pedidos.pedido(orderId, token);
        if (order == null || order.path("id").asLong() != orderId || !"PAGADA".equals(order.path("estado").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se asignan asesorías con pago confirmado.");
        JsonNode detail=order.path("detalles").path(position);
        if (!"ASESORIA".equals(detail.path("tipoEntrega").asText()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este detalle no corresponde a una asesoría.");
        Cliente person=cliente(order.path("idCliente").asText());
        // Serialize concurrent assignments for the same buyer; the unique index is the final invariant.
        person=clients.findForUpdate(person.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));
        var existing=cases.findByPedidoIdAndPedidoDetalle(orderId, position);
        if (existing.isPresent()) {
            if (!existing.get().getAbogado().getId().equals(lawyerId))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La atención ya está asignada. Reasigna desde el expediente.");
            return caseService.getById(existing.get().getId(), operatorEmail, "admin");
        }
        var lawyer=lawyers.findById(lawyerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Selecciona un abogado registrado."));
        var serviceName=detail.path("nombreServicio").asText();
        BigDecimal amount=detail.path("subtotal").decimalValue();
        if (serviceName.isBlank() || amount.signum()<=0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El detalle contratado no es válido.");
        Caso legalCase=new Caso();
        legalCase.setNombreCaso(serviceName + " · Pedido #" + orderId);
        legalCase.setCliente(person); legalCase.setAbogado(lawyer); legalCase.setEstado("Iniciado");
        legalCase.setFechaApertura(LocalDate.now()); legalCase.setTarifa(amount);
        legalCase.setDescripcion("Atención contratada: " + detail.path("cantidad").asInt() + " unidad(es)."
                + (description == null || description.isBlank() ? "" : "\n" + description.trim()));
        legalCase.vincularPedido(orderId, position, serviceName);
        legalCase=cases.saveAndFlush(legalCase);
        return caseService.getById(legalCase.getId(), operatorEmail, "admin");
    }

    private Cliente cliente(String subject) {
        return buscarCliente(subject).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                "El comprador no tiene un perfil de cliente vinculado."));
    }

    private Optional<Cliente> buscarCliente(String subject) {
        return identities.findByIssuerAndSubject(authority, subject)
                .flatMap(link -> clients.findByUserId(link.getUser().getId()));
    }

    public record Solicitud(long pedidoId, int posicion, String servicio, String cliente, Long clienteId,
            BigDecimal importe, int cantidad, String fecha, Long expedienteId, String abogado) {}
    public record Pagina(List<Solicitud> items, int pagina, boolean hayMas) {}
}
