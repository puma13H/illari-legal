package com.illarilegal.serviciolegal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios_legales")
public class ServicioLegalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false, length = 20)
    private String tipoEntrega;
    @Column(length = 64)
    private String archivoVersion;
    @Column(length = 255)
    private String archivoNombre;

    protected ServicioLegalEntity() {}

    public ServicioLegalEntity(String nombre, String descripcion, BigDecimal precio, String categoria, String estado) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
        if (this.tipoEntrega == null) this.tipoEntrega = com.illarilegal.compartido.catalogo.EntregaServicio.resolver(null, categoria).name();
        this.estado = estado;
    }

    public String getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(String value) { tipoEntrega = value; }
    public String getArchivoVersion() { return archivoVersion; }
    public void setArchivoVersion(String value) { archivoVersion = value; }
    public String getArchivoNombre() { return archivoNombre; }
    public void setArchivoNombre(String value) { archivoNombre = value; }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public String getCategoria() { return categoria; }
    public String getEstado() { return estado; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setEstado(String estado) { this.estado = estado; }
}
