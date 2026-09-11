package com.illarilegal.serviciolegal.controller;

import com.illarilegal.serviciolegal.dto.*;
import com.illarilegal.serviciolegal.service.ServicioLegalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioLegalController {
    private final ServicioLegalService service;
    public ServicioLegalController(ServicioLegalService service) { this.service = service; }

    @GetMapping public List<ServicioLegalResponse> listar() { return service.listar(); }
    @GetMapping("/{id}") public ServicioLegalResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    public ResponseEntity<ServicioLegalResponse> crear(@Valid @RequestBody ServicioLegalRequest request) {
        var created = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/servicios/" + created.id())).body(created);
    }
    @PutMapping("/{id}")
    public ServicioLegalResponse actualizar(@PathVariable Long id, @Valid @RequestBody ServicioLegalRequest request) {
        return service.actualizar(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id); return ResponseEntity.noContent().build();
    }
}
