package com.illarilegal.cliente.dto;

public record ClienteResponse(Long id, String dni, String nombres, String apellidos,
                              String email, String telefono, String direccion, String estado) {}
