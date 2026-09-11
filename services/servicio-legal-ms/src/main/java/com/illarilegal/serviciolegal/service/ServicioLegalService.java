package com.illarilegal.serviciolegal.service;

import com.illarilegal.serviciolegal.dto.*;
import com.illarilegal.serviciolegal.entity.ServicioLegal;
import com.illarilegal.serviciolegal.exception.ResourceNotFoundException;
import com.illarilegal.serviciolegal.repository.ServicioLegalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ServicioLegalService {
    private final ServicioLegalRepository repository;
    public ServicioLegalService(ServicioLegalRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<ServicioLegalResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly = true)
    public ServicioLegalResponse obtener(Long id) { return toResponse(find(id)); }

    public ServicioLegalResponse crear(ServicioLegalRequest r) {
        if (repository.existsByNombreIgnoreCase(r.nombre()))
            throw new IllegalArgumentException("Ya existe un servicio legal con ese nombre");
        ServicioLegal e = new ServicioLegal(); apply(e,r);
        return toResponse(repository.save(e));
    }
    public ServicioLegalResponse actualizar(Long id, ServicioLegalRequest r) {
        ServicioLegal e = find(id);
        if (repository.existsByNombreIgnoreCaseAndIdNot(r.nombre(), id))
            throw new IllegalArgumentException("Ya existe otro servicio legal con ese nombre");
        apply(e,r); return toResponse(repository.save(e));
    }
    public void eliminar(Long id) { repository.delete(find(id)); }

    private ServicioLegal find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Servicio legal " + id + " no encontrado"));
    }
    private void apply(ServicioLegal e, ServicioLegalRequest r) {
        e.setNombre(r.nombre()); e.setDescripcion(r.descripcion()); e.setPrecio(r.precio());
        e.setCategoria(r.categoria()); e.setEstado(r.estado());
    }
    private ServicioLegalResponse toResponse(ServicioLegal e) {
        return new ServicioLegalResponse(e.getId(),e.getNombre(),e.getDescripcion(),e.getPrecio(),e.getCategoria(),e.getEstado());
    }
}
