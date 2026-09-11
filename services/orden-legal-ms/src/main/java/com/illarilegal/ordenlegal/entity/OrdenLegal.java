package com.illarilegal.ordenlegal.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_legales")
public class OrdenLegal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cliente_id", nullable = false)
    private Long idCliente;
    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "orden_servicios", joinColumns = @JoinColumn(name = "orden_id"))
    @Column(name = "servicio_id", nullable = false)
    private List<Long> servicios = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdCliente() { return idCliente; }
    public void setIdCliente(Long idCliente) { this.idCliente = idCliente; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<Long> getServicios() { return servicios; }
    public void setServicios(List<Long> servicios) { this.servicios = servicios; }
}
