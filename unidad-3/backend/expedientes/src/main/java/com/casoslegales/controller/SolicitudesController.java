package com.casoslegales.controller;

import com.casoslegales.dto.CasoResponse;
import com.casoslegales.service.SolicitudesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solicitudes")
@PreAuthorize("hasRole('admin')")
public class SolicitudesController {
    private final SolicitudesService service;
    public SolicitudesController(SolicitudesService service) { this.service=service; }
    @GetMapping
    public SolicitudesService.Pagina listar(@RequestParam(defaultValue="0") int pagina, JwtAuthenticationToken auth) {
        return service.listar(pagina, auth.getToken().getTokenValue());
    }
    @PostMapping("/{pedido}/{posicion}/asignacion")
    public CasoResponse asignar(@PathVariable long pedido, @PathVariable int posicion,
            @RequestBody Asignacion body, JwtAuthenticationToken auth) {
        return service.asignar(pedido, posicion, body.abogadoId(), body.descripcion(),
                auth.getToken().getTokenValue(), auth.getName());
    }
    public record Asignacion(Long abogadoId, String descripcion) {}
}
