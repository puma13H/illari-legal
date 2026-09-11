package com.illarilegal.pago.controller;

import com.illarilegal.pago.dto.*;
import com.illarilegal.pago.service.PagoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos")
@Validated
public class PagoController {
    private final PagoService service;
    public PagoController(PagoService service) { this.service = service; }

    @GetMapping public List<PagoResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public PagoResponse obtener(@PathVariable @Positive Long id) { return service.obtener(id); }
    @GetMapping("/orden/{ordenId}")
    public List<PagoResponse> porOrden(@PathVariable @Positive Long ordenId) { return service.porOrden(ordenId); }

    @PostMapping
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody PagoRequest request) {
        var created = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/pagos/" + created.id())).body(created);
    }
    @PutMapping("/{id}")
    public PagoResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody PagoRequest request) {
        return service.actualizar(id, request);
    }
    @PatchMapping("/{id}/estado")
    public PagoResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoPagoRequest request) {
        return service.cambiarEstado(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        service.eliminar(id); return ResponseEntity.noContent().build();
    }
}
