package com.illarilegal.pago.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "orden_id", nullable = false)
    private Long idOrden;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Instant getFechaAprobacion() { return fechaAprobacion; }
    public void setFechaAprobacion(Instant fechaAprobacion) { this.fechaAprobacion = fechaAprobacion; }
}
