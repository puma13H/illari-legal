package com.illarilegal.pago.service;

import com.illarilegal.pago.dto.*;
import com.illarilegal.pago.entity.Pago;
import com.illarilegal.pago.exception.ResourceNotFoundException;
import com.illarilegal.pago.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PagoService {
    private final PagoRepository repository;
    public PagoService(PagoRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<PagoResponse> listar() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public PagoResponse obtener(Long id) { return toResponse(find(id)); }
    @Transactional(readOnly = true)
    public List<PagoResponse> porOrden(Long ordenId) {
        return repository.findByIdOrdenOrderByIdDesc(ordenId).stream().map(this::toResponse).toList();
    }

    public PagoResponse crear(PagoRequest r) {
        Pago e = new Pago(); e.setFechaCreacion(Instant.now()); apply(e,r);
        return toResponse(repository.save(e));
    }
    public PagoResponse actualizar(Long id, PagoRequest r) {
        Pago e = find(id); apply(e,r); return toResponse(repository.save(e));
    }
    public PagoResponse cambiarEstado(Long id, EstadoPagoRequest r) {
        Pago e = find(id); e.setEstado(r.estado());
        if ("APROBADO".equals(r.estado()) && e.getFechaAprobacion() == null) e.setFechaAprobacion(Instant.now());
        if (!"APROBADO".equals(r.estado())) e.setFechaAprobacion(null);
        return toResponse(repository.save(e));
    }
    public void eliminar(Long id) { repository.delete(find(id)); }

    private Pago find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pago " + id + " no encontrado"));
    }
    private void apply(Pago e, PagoRequest r) {
        e.setIdOrden(r.idOrden()); e.setMonto(r.monto()); e.setEstado(r.estado());
        if ("APROBADO".equals(r.estado()) && e.getFechaAprobacion() == null) e.setFechaAprobacion(Instant.now());
        if (!"APROBADO".equals(r.estado())) e.setFechaAprobacion(null);
    }
    private PagoResponse toResponse(Pago e) {
        return new PagoResponse(e.getId(),e.getIdOrden(),e.getMonto(),e.getEstado(),e.getFechaCreacion(),e.getFechaAprobacion());
    }
}
