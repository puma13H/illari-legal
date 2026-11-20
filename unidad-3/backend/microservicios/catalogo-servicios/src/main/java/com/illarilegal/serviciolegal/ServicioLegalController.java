package com.illarilegal.serviciolegal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioLegalController {
    private final ServicioLegalRepository servicios;
    private final ArchivosCatalogoService archivos;

    public ServicioLegalController(ServicioLegalRepository servicios, ArchivosCatalogoService archivos) {
        this.servicios = servicios;
        this.archivos = archivos;
    }

    @GetMapping
    public List<ServicioLegal> listar() {
        return servicios.findAll().stream()
                .sorted(Comparator.comparing(ServicioLegalEntity::getId))
                .map(this::aServicio)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioLegal> obtener(@PathVariable Long id) {
        return servicios.findById(id).map(this::aServicio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ServicioLegal crear(@RequestBody ServicioLegal request) {
        validar(request);
        String estado = normalizarEstado(request.estado() == null ? "ACTIVO" : request.estado());
        var entity = new ServicioLegalEntity(
                request.nombre().trim(),
                request.descripcion(),
                request.precio(),
                request.categoria().trim(),
                estado);
        entity.setTipoEntrega(tipo(request));
        validarEntrega(entity);
        return aServicio(servicios.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicioLegal> actualizar(@PathVariable Long id, @RequestBody ServicioLegal request) {
        var existente = servicios.findById(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        validar(request);
        ServicioLegalEntity servicio = existente.get();
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion());
        servicio.setPrecio(request.precio());
        servicio.setCategoria(request.categoria().trim());
        servicio.setTipoEntrega(tipo(request));
        if (request.estado() != null && !request.estado().isBlank()) {
            servicio.setEstado(normalizarEstado(request.estado()));
        }
        validarEntrega(servicio);
        return ResponseEntity.ok(aServicio(servicios.save(servicio)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ServicioLegal> estado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var existente = servicios.findById(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ServicioLegalEntity servicio = existente.get();
        if (body == null || !body.containsKey("estado")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requiere estado.");
        servicio.setEstado(normalizarEstado(body.get("estado")));
        validarEntrega(servicio);
        return ResponseEntity.ok(aServicio(servicios.save(servicio)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!servicios.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        servicios.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void validar(ServicioLegal servicio) {
        if (servicio == null || servicio.nombre() == null || servicio.nombre().isBlank()
                || servicio.precio() == null || servicio.precio().signum() < 0
                || servicio.precio().stripTrailingZeros().scale() > 2 || servicio.precio().compareTo(new BigDecimal("9999999999.99")) > 0
                || servicio.categoria() == null || servicio.categoria().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Nombre, categoria y un precio no negativo son obligatorios.");
        }
    }

    private String normalizarEstado(String estado) {
        String e = estado == null ? "" : estado.trim().toUpperCase(java.util.Locale.ROOT);
        if ("ACTIVA".equals(e)) e="ACTIVO"; if ("INACTIVA".equals(e)) e="INACTIVO";
        if (!java.util.Set.of("ACTIVO","INACTIVO").contains(e)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado no admitido.");
        return e;
    }

    private String tipo(ServicioLegal request) {
        try { return com.illarilegal.compartido.catalogo.EntregaServicio.resolver(request.tipoEntrega(), request.categoria()).name(); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona asesoría o recurso digital."); }
    }

    private void validarEntrega(ServicioLegalEntity service) {
        if ("ACTIVO".equals(service.getEstado()) && "ARCHIVO".equals(service.getTipoEntrega()) && !archivos.disponible(service))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Adjunta el PDF antes de publicar este recurso digital.");
    }

    @PostMapping(value="/{id}/archivo", consumes="multipart/form-data")
    public ServicioLegal subirArchivo(@PathVariable Long id, @RequestParam("archivo") org.springframework.web.multipart.MultipartFile file) {
        return aServicio(archivos.guardar(id, file));
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<org.springframework.core.io.Resource> descargarArchivo(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"Recurso-ILLARI-LEGAL.pdf\"")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(org.springframework.http.CacheControl.noStore()).body(archivos.obtener(id));
    }
    private ServicioLegal aServicio(ServicioLegalEntity entity) {
        return new ServicioLegal(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getPrecio(),
                entity.getCategoria(),
                entity.getEstado(), entity.getTipoEntrega(), entity.getArchivoVersion(), entity.getArchivoNombre(), archivos.disponible(entity));
    }

    public record ServicioLegal(Long id, String nombre, String descripcion, BigDecimal precio, String categoria, String estado,
            String tipoEntrega, String archivoVersion, String archivoNombre, boolean archivoDisponible) {}
}
