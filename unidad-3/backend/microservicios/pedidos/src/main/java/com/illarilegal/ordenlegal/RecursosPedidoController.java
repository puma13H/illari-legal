package com.illarilegal.ordenlegal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.util.*;

/** Los archivos se entregan únicamente al titular o al administrador de un pedido pagado. */
@RestController
@RequestMapping("/api/v1/ordenes")
public class RecursosPedidoController {
    private final OrdenRepository orders;
    private final ObjectMapper json;
    private final long guideService;
    private final Path files;

    @Autowired
    public RecursosPedidoController(OrdenRepository orders, ObjectMapper json,
            @Value("${ILLARI_GUIA_SERVICIO_ID:0}") long guideService,
            @Value("${ILLARI_CATALOGO_ARCHIVOS_DIR:/app/catalogo-archivos}") String directory) {
        this.orders = orders;
        this.json = json;
        this.guideService = guideService;
        this.files = Path.of(directory).toAbsolutePath().normalize();
    }

    RecursosPedidoController(OrdenRepository orders, ObjectMapper json, long guideService) {
        this(orders, json, guideService, System.getProperty("java.io.tmpdir"));
    }

    @GetMapping("/{id}/recursos")
    @Transactional(readOnly = true)
    public List<Recurso> recursos(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        OrdenEntity order = owned(id, jwt);
        if (!"PAGADA".equals(order.getEstado())) return List.of();
        return detalles(order).stream()
                .filter(d -> d.idServicio() != null && (d.idServicio() == guideService
                        || "ARCHIVO".equals(d.tipoEntrega()) && d.nombreServicio() != null))
                .map(d -> new Recurso(d.idServicio() == guideService ? "guia-bienvenida" : "recurso-" + d.idServicio(),
                        d.idServicio() == guideService ? "Guía de bienvenida digital" : d.nombreServicio(),
                        "PDF", d.idServicio(), disponible(d)))
                .distinct().toList();
    }

    @GetMapping("/{id}/recursos/guia-bienvenida")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return descargarRecurso(id, guideService, jwt);
    }

    @GetMapping("/{id}/recursos/servicios/{servicio}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> descargarRecurso(@PathVariable Long id, @PathVariable Long servicio,
            @AuthenticationPrincipal Jwt jwt) {
        OrdenEntity order = owned(id, jwt);
        if (!"PAGADA".equals(order.getEstado())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El recurso estará disponible cuando se confirme el pago.");
        var detail = detalles(order).stream().filter(d -> servicio.equals(d.idServicio())
                && (servicio == guideService || "ARCHIVO".equals(d.tipoEntrega())))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!disponible(detail)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El archivo de este pedido todavía está en preparación.");
        boolean guide = esGuia(detail);
        Resource resource = guide ? new ClassPathResource("guias/guia-bienvenida.pdf")
                : new FileSystemResource(files.resolve(detail.archivoVersion() + ".pdf"));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + (guide ? "Guia-de-bienvenida-ILLARI-LEGAL.pdf" : "Recurso-ILLARI-LEGAL.pdf") + "\"")
                .body(resource);
    }

    private boolean esGuia(OrdenLegalController.DetalleOrden detail) {
        return "guia-bienvenida".equals(detail.archivoVersion())
                || detail.idServicio() == guideService && detail.archivoVersion() == null;
    }

    private boolean disponible(OrdenLegalController.DetalleOrden detail) {
        String version = detail.archivoVersion();
        return esGuia(detail) || version != null && version.matches("[a-f0-9-]{36}")
                && Files.isRegularFile(files.resolve(version + ".pdf"));
    }

    private List<OrdenLegalController.DetalleOrden> detalles(OrdenEntity order) {
        if (!order.getDetalles().isEmpty()) return order.getDetalles().stream().map(OrdenDetalleEntity::snapshot).toList();
        try {
            return json.readValue(order.getDetallesJson(), json.getTypeFactory()
                    .constructCollectionType(List.class, OrdenLegalController.DetalleOrden.class));
        } catch (java.io.IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron consultar los recursos.");
        }
    }

    private OrdenEntity owned(Long id, Jwt jwt) {
        Object access = jwt.getClaim("realm_access");
        boolean admin = access instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles
                && roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(String.valueOf(r)));
        return orders.findById(id).filter(o -> admin || o.getIdCliente().equals(jwt.getSubject()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record Recurso(String codigo, String titulo, String formato, long idServicio, boolean disponible) {}
}
