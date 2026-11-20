package com.illarilegal.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "pagos_legales",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pago_operacion_transferencia",
                    columnNames = "operacion_conciliada"),
            @UniqueConstraint(name = "uk_pago_yape_token", columnNames = "yape_token_huella"),
            @UniqueConstraint(name = "uk_pago_yape_orden", columnNames = "yape_orden_proveedor")
        })
public class PagoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long idOrden;

    @Column(nullable = false)
    private String idCliente;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private Instant fechaCreacion;

    private Instant fechaAprobacion;

    @Column(name = "mercado_pago_id", length = 64, insertable = false, updatable = false)
    private String proveedorAnteriorId;

    @Column(length = 10)
    private String metodoTransferencia;

    @Column(length = 160)
    private String titularDestino;

    @Column(length = 9)
    private String celularDestino;

    @Column(length = 64)
    private String codigoOperacion;

    private String confirmadoPor;

    @Column(length = 74)
    private String operacionConciliada;

    @Column(name = "yape_culqi", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("false")
    private boolean yapeAnterior;

    @Column(name = "culqi_intento", length = 36, insertable = false, updatable = false)
    private String intentoYapeAnterior;

    @Column(name = "culqi_cargo_id", length = 25, insertable = false, updatable = false)
    private String cargoYapeAnterior;

    @Column(nullable = false)
    private boolean yapeCheckoutApi;

    @Column(length = 36)
    private String yapeIntento;
    private Instant yapeFechaIntento;
    private Instant yapeUltimaRevision;
    void registrarRevisionYape() { yapeUltimaRevision = Instant.now(); }

    @Column(length = 4)
    private String yapeAmbiente;

    @Column(length = 64)
    private String yapeTokenHuella;

    @Column(length = 64)
    private String yapeOrdenProveedor;

    @Column(length = 64)
    private String yapeRechazoCodigo;

    @Column(length = 64)
    private String yapeRechazoReferencia;

    private Instant yapeRechazoFecha;

    protected PagoEntity() {}

    public PagoEntity(
            Long idOrden,
            String idCliente,
            BigDecimal monto,
            String estado,
            Instant fechaCreacion,
            Instant fechaAprobacion) {
        this.idOrden = idOrden;
        this.idCliente = idCliente;
        this.monto = monto;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaAprobacion = fechaAprobacion;
    }

    public Long getId() {
        return id;
    }

    public Long getIdOrden() {
        return idOrden;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getEstado() {
        return estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setFechaAprobacion(Instant fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public boolean esPagoExterno() {
        return esYapeEnLinea() || metodoTransferencia != null || proveedorAnteriorId != null;
    }

    public String getMetodoTransferencia() {
        return metodoTransferencia;
    }

    public String getTitularDestino() {
        return titularDestino;
    }

    public String getCelularDestino() {
        return celularDestino;
    }

    public String getCodigoOperacion() {
        return codigoOperacion;
    }

    public void setCodigoOperacion(String codigoOperacion) {
        this.codigoOperacion = codigoOperacion;
    }

    public void conciliar(String operador) {
        estado = "VALIDADO";
        fechaAprobacion = Instant.now();
        confirmadoPor = operador;
        operacionConciliada = celularDestino + ":" + codigoOperacion;
    }

    void prepararTransferencia(String metodo, String titular, String celular) {
        metodoTransferencia = metodo;
        titularDestino = titular;
        celularDestino = celular;
    }

    boolean esYapeEnLinea() {
        return yapeCheckoutApi || yapeAnterior;
    }

    boolean esYapeCheckoutApi() {
        return yapeCheckoutApi;
    }

    void prepararYape() {
        yapeCheckoutApi = true;
    }

    String getYapeIntento() {
        return yapeCheckoutApi ? yapeIntento : intentoYapeAnterior;
    }

    String getYapeAmbiente() {
        return yapeAmbiente;
    }
    Instant getYapeFechaIntento() { return yapeFechaIntento == null ? fechaCreacion : yapeFechaIntento; }

    String getYapeOrdenProveedor() {
        return yapeCheckoutApi ? yapeOrdenProveedor : cargoYapeAnterior;
    }

    String getYapeRechazoCodigo() {
        return yapeRechazoCodigo;
    }

    void reservarCargoYape(String intento, String ambiente, String tokenHuella) {
        yapeIntento = intento;
        yapeFechaIntento = Instant.now();
        yapeAmbiente = ambiente;
        yapeTokenHuella = tokenHuella;
    }

    void liberarCargoRechazado() {
        yapeIntento = null;
        yapeFechaIntento = null;
        yapeAmbiente = null;
        yapeTokenHuella = null;
        limpiarRechazo();
    }

    void registrarRechazoImporte(String requestId) {
        yapeRechazoCodigo = "invalid_transaction_amount";
        yapeRechazoReferencia = requestId;
        yapeRechazoFecha = Instant.now();
    }

    private void limpiarRechazo() {
        yapeRechazoCodigo = null;
        yapeRechazoReferencia = null;
        yapeRechazoFecha = null;
    }

    void confirmarCargoYape(String ordenId) {
        yapeOrdenProveedor = ordenId;
        estado = "VALIDADO";
        fechaAprobacion = Instant.now();
        limpiarRechazo();
    }
}
