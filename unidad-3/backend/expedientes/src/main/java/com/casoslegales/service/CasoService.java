package com.casoslegales.service;

import com.casoslegales.dto.CasoRequest;
import com.casoslegales.dto.CasoResponse;
import com.casoslegales.dto.SeguimientoRequest;
import com.casoslegales.model.*;
import com.casoslegales.repository.*;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CasoService {

    private final CasoRepository casoRepository;
    private final ClienteRepository clienteRepository;
    private final AbogadoRepository abogadoRepository;
    private final UserRepository userRepository;

    public CasoService(
            CasoRepository casoRepository,
            ClienteRepository clienteRepository,
            AbogadoRepository abogadoRepository,
            UserRepository userRepository) {
        this.casoRepository = casoRepository;
        this.clienteRepository = clienteRepository;
        this.abogadoRepository = abogadoRepository;
        this.userRepository = userRepository;
    }

    public List<CasoResponse> listForUser(String email, String role) {
        return switch (role) {
            case "admin" -> casoRepository.findAll().stream().map(this::toResponse).toList();
            case "abogado" -> {
                Abogado abogado =
                        abogadoRepository
                                .findByUserId(getUserId(email))
                                .orElseThrow(() -> new RuntimeException("Abogado no encontrado"));
                yield casoRepository.findByAbogadoIdOrderByIdDesc(abogado.getId()).stream()
                        .map(this::toResponse)
                        .toList();
            }
            case "cliente" -> {
                Cliente cliente =
                        clienteRepository
                                .findByUserId(getUserId(email))
                                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
                yield casoRepository.findByClienteIdOrderByIdDesc(cliente.getId()).stream()
                        .map(this::toResponse)
                        .toList();
            }
            default -> throw new RuntimeException("Rol no soportado");
        };
    }

    @org.springframework.transaction.annotation.Transactional
    public CasoResponse create(CasoRequest request, String email, String role) {
        validar(request);
        Abogado abogado;
        if ("admin".equals(role)) {
            if (request.abogadoId() == null) {
                throw new RuntimeException("Abogado es requerido");
            }
            abogado =
                    abogadoRepository
                            .findById(request.abogadoId())
                            .orElseThrow(() -> new RuntimeException("Abogado no encontrado"));
        } else {
            abogado =
                    abogadoRepository
                            .findByUserId(getUserId(email))
                            .orElseThrow(() -> new RuntimeException("Abogado no encontrado"));
            if (request.abogadoId() != null && !request.abogadoId().equals(abogado.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "No puedes asignar otro abogado");
            }
        }

        Cliente cliente =
                clienteRepository
                        .findById(request.clienteId())
                        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Caso caso = new Caso();
        caso.setNombreCaso(request.nombreCaso());
        caso.setCliente(cliente);
        caso.setAbogado(abogado);
        caso.setEstado(request.estado());
        caso.setFechaApertura(request.fechaApertura());
        caso.setDescripcion(request.descripcion());
        caso.setTarifa(request.tarifa());
        caso.setFechaAudiencia(request.fechaAudiencia());

        return toResponse(casoRepository.save(caso));
    }

    public CasoResponse getById(Long id, String email, String role) {
        Caso caso =
                casoRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        validateAccess(caso, email, role);
        return toResponse(caso);
    }

    @org.springframework.transaction.annotation.Transactional
    public CasoResponse seguimiento(
            Long id, SeguimientoRequest request, String email, String role) {
        if (!"admin".equals(role) && !"abogado".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No puedes editar el seguimiento.");
        }
        Caso caso =
                casoRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new org.springframework.web.server.ResponseStatusException(
                                                org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Caso no encontrado."));
        validateAccess(caso, email, role);
        if (request == null
                || request.estado() == null
                || !java.util.Set.of("Iniciado", "En trámite", "En revisión", "Cerrado")
                        .contains(request.estado())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Selecciona un estado válido.");
        }
        if (request.fechaAudiencia() != null
                && request.fechaAudiencia().isBefore(caso.getFechaApertura())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "La audiencia no puede ser anterior a la apertura del expediente.");
        }
        caso.setEstado(request.estado());
        caso.setFechaAudiencia(request.fechaAudiencia());
        return toResponse(casoRepository.save(caso));
    }

    @org.springframework.transaction.annotation.Transactional
    public CasoResponse update(Long id, CasoRequest request, String email, String role) {
        validar(request);
        Caso caso =
                casoRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        validateAccess(caso, email, role);

        if (!"admin".equals(role) && !"abogado".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para editar casos");
        }

        if (caso.getPedidoId() != null && !caso.getCliente().getId().equals(request.clienteId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "El cliente de una asesoría pagada es el titular del pedido.");
        if ("admin".equals(role)) {
            caso.setAbogado(
                    abogadoRepository
                            .findById(request.abogadoId())
                            .orElseThrow(
                                    () ->
                                            new org.springframework.web.server
                                                    .ResponseStatusException(
                                                    org.springframework.http.HttpStatus.BAD_REQUEST,
                                                    "Selecciona un abogado registrado.")));
        } else if (!caso.getAbogado().getId().equals(request.abogadoId())
                || !caso.getCliente().getId().equals(request.clienteId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La asignación de personas corresponde al administrador.");
        }

        Cliente cliente =
                clienteRepository
                        .findById(request.clienteId())
                        .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        caso.setNombreCaso(request.nombreCaso());
        caso.setCliente(cliente);
        caso.setEstado(request.estado());
        caso.setFechaApertura(request.fechaApertura());
        caso.setDescripcion(request.descripcion());
        caso.setTarifa(request.tarifa());
        caso.setFechaAudiencia(request.fechaAudiencia());

        return toResponse(casoRepository.save(caso));
    }

    public void delete(Long id, String email, String role) {
        Caso caso =
                casoRepository
                        .findById(id)
                        .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        validateAccess(caso, email, role);
        if (!"abogado".equals(role) && !"admin".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permiso para eliminar casos");
        }
        casoRepository.delete(caso);
    }

    private void validateAccess(Caso caso, String email, String role) {
        if ("admin".equals(role)) return;

        if ("abogado".equals(role)) {
            Abogado abogado =
                    abogadoRepository
                            .findByUserId(getUserId(email))
                            .orElseThrow(() -> new RuntimeException("Abogado no encontrado"));
            if (!caso.getAbogado().getId().equals(abogado.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Acceso no autorizado");
            }
            return;
        }

        if ("cliente".equals(role)) {
            Cliente cliente =
                    clienteRepository
                            .findByUserId(getUserId(email))
                            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            if (!caso.getCliente().getId().equals(cliente.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Acceso no autorizado");
            }
            return;
        }

        throw new org.springframework.security.access.AccessDeniedException("Acceso no autorizado");
    }

    private Long getUserId(String email) {
        return userRepository
                .findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private CasoResponse toResponse(Caso caso) {
        return new CasoResponse(
                caso.getId(),
                caso.getNombreCaso(),
                caso.getCliente().getId(),
                caso.getCliente().getNombre(),
                caso.getAbogado().getId(),
                caso.getAbogado().getNombre(),
                caso.getEstado(),
                caso.getFechaApertura(),
                caso.getDescripcion(),
                caso.getTarifa(),
                caso.getFechaAudiencia(),
                caso.isReferencia(),
                caso.getAbogado().getEmail(),
                caso.getAbogado().getTelefono(), caso.getPedidoId(), caso.getServicioContratado());
    }

    private void validar(CasoRequest request) {
        if (request == null
                || request.nombreCaso() == null
                || request.nombreCaso().isBlank()
                || request.estado() == null
                || !java.util.Set.of("Iniciado", "En trámite", "En revisión", "Cerrado")
                        .contains(request.estado())
                || request.clienteId() == null
                || request.abogadoId() == null
                || request.fechaApertura() == null
                || request.tarifa() == null
                || request.tarifa().signum() < 0
                || (request.fechaAudiencia() != null
                        && request.fechaAudiencia().isBefore(request.fechaApertura()))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Revisa el nombre, las personas asignadas, el estado, el importe y las fechas"
                        + " del expediente.");
        }
    }
}
