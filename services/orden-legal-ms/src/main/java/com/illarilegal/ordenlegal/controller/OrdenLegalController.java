package com.illarilegal.ordenlegal.controller;

import com.illarilegal.ordenlegal.dto.*;
import com.illarilegal.ordenlegal.service.OrdenLegalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ordenes")
@Validated
public class OrdenLegalController {
    private final OrdenLegalService service;
    public OrdenLegalController(OrdenLegalService service) { this.service = service; }

    @GetMapping public List<OrdenLegalResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public OrdenLegalResponse obtener(@PathVariable @Positive Long id) { return service.obtener(id); }
    @GetMapping("/cliente/{clienteId}")
    public List<OrdenLegalResponse> porCliente(@PathVariable @Positive Long clienteId) { return service.porCliente(clienteId); }

    @PostMapping
    public ResponseEntity<OrdenLegalResponse> crear(@Valid @RequestBody OrdenLegalRequest request) {
        var created = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/ordenes/" + created.id())).body(created);
    }
    @PutMapping("/{id}")
    public OrdenLegalResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody OrdenLegalRequest request) {
        return service.actualizar(id, request);
    }
    @PatchMapping("/{id}/estado")
    public OrdenLegalResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoOrdenRequest request) {
        return service.cambiarEstado(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        service.eliminar(id); return ResponseEntity.noContent().build();
    }
}
