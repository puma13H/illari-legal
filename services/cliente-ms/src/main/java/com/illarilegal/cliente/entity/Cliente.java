package com.illarilegal.cliente.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(name = "uk_clientes_dni", columnNames = "dni"))
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String dni;
    @Column(nullable = false, length = 120)
    private String nombres;
    @Column(nullable = false, length = 120)
    private String apellidos;
    @Column(length = 160)
    private String email;
    @Column(length = 30)
    private String telefono;
    @Column(length = 240)
    private String direccion;
    @Column(nullable = false, length = 20)
    private String estado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
