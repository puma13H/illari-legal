package com.illarilegal.cliente.service;

import com.illarilegal.cliente.dto.ClienteRequest;
import com.illarilegal.cliente.dto.ClienteResponse;
import com.illarilegal.cliente.entity.Cliente;
import com.illarilegal.cliente.exception.ResourceNotFoundException;
import com.illarilegal.cliente.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long id) {
        return toResponse(find(id));
    }

    public ClienteResponse crear(ClienteRequest request) {
        if (repository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("Ya existe un cliente con el DNI indicado");
        }
        Cliente entity = new Cliente();
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    public ClienteResponse actualizar(Long id, ClienteRequest request) {
        Cliente entity = find(id);
        if (repository.existsByDniAndIdNot(request.dni(), id)) {
            throw new IllegalArgumentException("Ya existe otro cliente con el DNI indicado");
        }
        apply(entity, request);
        return toResponse(repository.save(entity));
    }

    public void eliminar(Long id) {
        Cliente entity = find(id);
        repository.delete(entity);
    }

    private Cliente find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente " + id + " no encontrado"));
    }

    private void apply(Cliente e, ClienteRequest r) {
        e.setDni(r.dni());
        e.setNombres(r.nombres());
        e.setApellidos(r.apellidos());
        e.setEmail(r.email());
        e.setTelefono(r.telefono());
        e.setDireccion(r.direccion());
        e.setEstado(r.estado());
    }

    private ClienteResponse toResponse(Cliente e) {
        return new ClienteResponse(e.getId(), e.getDni(), e.getNombres(), e.getApellidos(),
                e.getEmail(), e.getTelefono(), e.getDireccion(), e.getEstado());
    }
}
