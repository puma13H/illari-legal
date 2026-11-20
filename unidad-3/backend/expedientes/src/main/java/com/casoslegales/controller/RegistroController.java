package com.casoslegales.controller;

import com.casoslegales.dto.*;
import com.casoslegales.service.RegistroService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/registros")
@PreAuthorize("hasRole('admin')")
public class RegistroController {
    private final RegistroService service;

    public RegistroController(RegistroService service) {
        this.service = service;
    }

    @PostMapping("/{tipo}")
    public RegistroResponse create(
            @PathVariable String tipo,
            @Valid @RequestBody RegistroRequest request,
            Authentication auth) {
        return service.create(tipo, request, auth.getName());
    }

    @PutMapping("/{tipo}/{id}")
    public RegistroResponse update(
            @PathVariable String tipo,
            @PathVariable Long id,
            @Valid @RequestBody RegistroRequest request) {
        return service.update(tipo, id, request);
    }
}
