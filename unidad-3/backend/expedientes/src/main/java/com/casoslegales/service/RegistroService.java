package com.casoslegales.service;

import com.casoslegales.dto.*;
import com.casoslegales.model.*;
import com.casoslegales.repository.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RegistroService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final ClienteRepository clients;
    private final AbogadoRepository lawyers;
    private final IdentidadKeycloakRepository identities;
    private final AccesoRegistroClient access;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final String issuer;

    public RegistroService(
            UserRepository users,
            RoleRepository roles,
            ClienteRepository clients,
            AbogadoRepository lawyers,
            IdentidadKeycloakRepository identities,
            AccesoRegistroClient access,
            PasswordEncoder encoder,
            PlatformTransactionManager manager,
            @Value(
                            "${ILLARI_IDENTITY_AUTHORITY:${spring.security.oauth2.resourceserver.jwt.issuer-uri}}")
                    String issuer) {
        this.users = users;
        this.roles = roles;
        this.clients = clients;
        this.lawyers = lawyers;
        this.identities = identities;
        this.access = access;
        this.encoder = encoder;
        this.transaction = new TransactionTemplate(manager);
        this.issuer = issuer;
    }

    public RegistroResponse create(String type, RegistroRequest input, String operator) {
        String role = role(type);
        validate(input, role, true);
        if (users.existsByEmail(email(input)))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya pertenece a un registro.");
        String subject = access.crear(input, role.toUpperCase(Locale.ROOT));
        try {
            return transaction.execute(
                    status -> {
                        User user = new User();
                        user.setName(input.nombre().trim());
                        user.setEmail(email(input));
                        user.setRole(roles.findByName(role).orElseThrow());
                        user.setPassword(
                                encoder.encode(UUID.randomUUID() + UUID.randomUUID().toString()));
                        user = users.saveAndFlush(user);
                        Long id;
                        if (role.equals("cliente")) {
                            Cliente p = new Cliente();
                            fill(p, input);
                            p.setUser(user);
                            id = clients.saveAndFlush(p).getId();
                        } else {
                            Abogado p = new Abogado();
                            fill(p, input);
                            p.setUser(user);
                            id = lawyers.saveAndFlush(p).getId();
                        }
                        identities.saveAndFlush(
                                new IdentidadKeycloak(issuer, subject, user, operator));
                        access.activar(subject);
                        return new RegistroResponse(
                                id, input.nombre().trim(), email(input), input.usuario(), true);
                    });
        } catch (RuntimeException ex) {
            try {
                access.eliminarCreado(subject);
            } catch (RuntimeException cleanup) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "El registro no se completó. Solicita revisar la cuenta antes de"
                            + " reintentar.");
            }
            throw ex;
        }
    }

    public RegistroResponse update(String type, Long id, RegistroRequest input) {
        String role = role(type);
        validate(input, role, false);
        java.util.concurrent.atomic.AtomicReference<String> changedSubject =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Map<String, Object>> previousData =
                new java.util.concurrent.atomic.AtomicReference<>();
        try {
            return transaction.execute(
                    status -> {
                        User user;
                        Cliente client = null;
                        Abogado lawyer = null;
                        if (role.equals("cliente")) {
                            client =
                                    clients.findById(id)
                                            .orElseThrow(
                                                    () ->
                                                            new ResponseStatusException(
                                                                    HttpStatus.NOT_FOUND));
                            user = client.getUser();
                        } else {
                            lawyer =
                                    lawyers.findById(id)
                                            .orElseThrow(
                                                    () ->
                                                            new ResponseStatusException(
                                                                    HttpStatus.NOT_FOUND));
                            user = lawyer.getUser();
                        }
                        if (user == null || !role.equals(user.getRole().getName()))
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "El registro no tiene un perfil de acceso válido.");
                        var duplicate = users.findByEmail(email(input));
                        if (duplicate.isPresent() && !duplicate.get().getId().equals(user.getId()))
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT, "El correo ya está registrado.");
                        var link = identities.findByUserIdAndIssuer(user.getId(), issuer);
                        Map<String, Object> previous =
                                link.map(l -> access.obtener(l.getSubject())).orElse(null);
                        if (link.isPresent()) {
                            changedSubject.set(link.get().getSubject());
                            previousData.set(previous);
                            access.actualizar(link.get().getSubject(), input);
                        }
                        user.setName(input.nombre().trim());
                        user.setEmail(email(input));
                        users.saveAndFlush(user);
                        if (client != null) {
                            fill(client, input);
                            clients.saveAndFlush(client);
                        } else {
                            fill(lawyer, input);
                            lawyers.saveAndFlush(lawyer);
                        }
                        return new RegistroResponse(
                                id,
                                input.nombre().trim(),
                                email(input),
                                previous == null ? null : String.valueOf(previous.get("username")),
                                link.isPresent());
                    });
        } catch (RuntimeException ex) {
            if (changedSubject.get() != null)
                access.restaurar(changedSubject.get(), previousData.get());
            throw ex;
        }
    }

    private void validate(RegistroRequest input, String role, boolean creation) {
        if (input.nombre().isBlank()
                || (role.equals("abogado")
                        && (input.especialidad() == null || input.especialidad().isBlank())))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Completa el nombre y la especialidad del profesional.");
        if (creation
                && (input.usuario() == null
                        || input.usuario().isBlank()
                        || input.claveInicial() == null
                        || input.claveInicial().length() < 12))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario y una contraseña inicial de al menos 12 caracteres son"
                        + " obligatorios.");
    }

    private String role(String type) {
        return switch (type) {
            case "clientes" -> "cliente";
            case "abogados" -> "abogado";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    private String email(RegistroRequest input) {
        return input.email().trim().toLowerCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private void fill(Cliente p, RegistroRequest i) {
        p.setNombre(i.nombre().trim());
        p.setEmail(email(i));
        p.setTelefono(text(i.telefono()));
        p.setDireccion(text(i.direccion()));
    }

    private void fill(Abogado p, RegistroRequest i) {
        p.setNombre(i.nombre().trim());
        p.setEmail(email(i));
        p.setTelefono(text(i.telefono()));
        p.setDireccion(text(i.direccion()));
        p.setEspecialidad(text(i.especialidad()));
    }
}
