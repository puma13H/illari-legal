package com.illarilegal.ordenlegal.service;

import com.illarilegal.ordenlegal.dto.*;
import com.illarilegal.ordenlegal.entity.OrdenLegal;
import com.illarilegal.ordenlegal.exception.ResourceNotFoundException;
import com.illarilegal.ordenlegal.repository.OrdenLegalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrdenLegalService {
    private final OrdenLegalRepository repository;
    public OrdenLegalService(OrdenLegalRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<OrdenLegalResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public OrdenLegalResponse obtener(Long id) { return toResponse(find(id)); }
    @Transactional(readOnly = true)
    public List<OrdenLegalResponse> porCliente(Long clienteId) {
        return repository.findByIdClienteOrderByIdDesc(clienteId).stream().map(this::toResponse).toList();
    }

    public OrdenLegalResponse crear(OrdenLegalRequest r) {
        OrdenLegal e = new OrdenLegal();
        e.setFechaCreacion(Instant.now());
        apply(e, r);
        return toResponse(repository.save(e));
    }
    public OrdenLegalResponse actualizar(Long id, OrdenLegalRequest r) {
        OrdenLegal e = find(id);
        apply(e, r);
        return toResponse(repository.save(e));
    }
    public OrdenLegalResponse cambiarEstado(Long id, EstadoOrdenRequest r) {
        OrdenLegal e = find(id);
        e.setEstado(r.estado());
        return toResponse(repository.save(e));
    }
    public void eliminar(Long id) { repository.delete(find(id)); }

    private OrdenLegal find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Orden legal " + id + " no encontrada"));
    }
    private void apply(OrdenLegal e, OrdenLegalRequest r) {
        e.setIdCliente(r.idCliente());
        e.setServicios(new ArrayList<>(r.servicios()));
        e.setTotal(r.total());
        e.setEstado(r.estado());
    }
    private OrdenLegalResponse toResponse(OrdenLegal e) {
        return new OrdenLegalResponse(e.getId(), e.getIdCliente(), List.copyOf(e.getServicios()),
                e.getTotal(), e.getEstado(), e.getFechaCreacion());
    }
}
