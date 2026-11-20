package com.casoslegales.dto;

public record RegistroResponse(
        Long id, String nombre, String email, String usuario, boolean accesoHabilitado) {}
