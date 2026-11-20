package com.illarilegal.ordenlegal;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orden_detalle", uniqueConstraints = @UniqueConstraint(name = "uk_detalle_posicion", columnNames = {"orden_id", "posicion"}))
@org.hibernate.annotations.Check(constraints = "cantidad > 0 AND precio_unitario >= 0 AND subtotal = precio_unitario * cantidad")
public class OrdenDetalleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false) private OrdenEntity orden;
    @Column(nullable = false) private int posicion;
    @Column(name = "servicio_id", nullable = false) private Long servicioId;
    @Column(nullable = false) private String nombreHistorico;
    @Column(nullable = false) private Integer cantidad;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, length = 20) private String tipoEntrega;
    @Column(length = 64) private String archivoVersion;
    @Column(length = 255) private String archivoNombre;
    protected OrdenDetalleEntity() {}
    public OrdenDetalleEntity(OrdenEntity orden, int posicion, OrdenLegalController.DetalleOrden d) {
        this.orden = orden; this.posicion = posicion; this.servicioId = d.idServicio();
        this.nombreHistorico = d.nombreServicio(); this.cantidad = d.cantidad();
        this.precioUnitario = d.precioUnitario(); this.subtotal = d.subtotal();
        this.tipoEntrega = d.tipoEntrega(); this.archivoVersion = d.archivoVersion(); this.archivoNombre = d.archivoNombre();
    }
    public OrdenLegalController.DetalleOrden snapshot() {
        return new OrdenLegalController.DetalleOrden(servicioId, nombreHistorico, cantidad, precioUnitario, subtotal, tipoEntrega, archivoVersion, archivoNombre);
    }
    public Long getServicioId() { return servicioId; }
    public Integer getCantidad() { return cantidad; }
}

