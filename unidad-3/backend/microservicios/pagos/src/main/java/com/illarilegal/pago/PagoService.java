package com.illarilegal.pago;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PagoService {
    private final PagoRepository pagos;
    private final ResultadoPagoOutbox resultados;

    public PagoService(PagoRepository pagos, ResultadoPagoOutbox resultados) {
        this.pagos = pagos;
        this.resultados = resultados;
    }

    @Transactional
    public void iniciarYape(Long idOrden, String idCliente, BigDecimal monto) {
        validarDatos(idOrden, idCliente, monto);
        var existente = pagos.findByOrdenForUpdate(idOrden);
        if (existente.isPresent()) {
            PagoEntity entity = existente.get();
            validarCorrespondencia(entity, idCliente, monto);
            if (!entity.esYapeEnLinea()) {
                throw new IllegalArgumentException("El evento Yape contradice el pago existente.");
            }
            return;
        }
        var entity = new PagoEntity(idOrden, idCliente, monto, "PENDIENTE", Instant.now(), null);
        entity.prepararYape();
        pagos.saveAndFlush(entity);
    }

    @Transactional
    public void iniciarTransferencia(
            Long idOrden,
            String idCliente,
            BigDecimal monto,
            String metodo,
            OrdenEventConsumer.DestinoTransferencia destino) {
        validarDatos(idOrden, idCliente, monto);
        if (!Set.of("YAPE", "PLIN", "BIPAY").contains(metodo)
                || destino == null
                || destino.titular() == null
                || destino.titular().isBlank()
                || destino.titular().length() > 160
                || destino.celular() == null
                || !destino.celular().matches("9[0-9]{8}")) {
            throw new IllegalArgumentException(
                    "El evento requiere un destino de transferencia válido.");
        }
        var existente = pagos.findByOrdenForUpdate(idOrden);
        if (existente.isPresent()) {
            PagoEntity entity = existente.get();
            validarCorrespondencia(entity, idCliente, monto);
            if (!metodo.equals(entity.getMetodoTransferencia())
                    || !destino.titular().equals(entity.getTitularDestino())
                    || !destino.celular().equals(entity.getCelularDestino())) {
                throw new IllegalArgumentException("El destino contradice el pago existente.");
            }
            return;
        }
        var entity = new PagoEntity(idOrden, idCliente, monto, "PENDIENTE", Instant.now(), null);
        entity.prepararTransferencia(metodo, destino.titular(), destino.celular());
        pagos.saveAndFlush(entity);
    }

    @Transactional
    public Pago informarOperacion(Long idPago, String idCliente, String referencia) {
        PagoEntity entity = buscarParaActualizar(idPago);
        if (!entity.getIdCliente().equals(idCliente)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago inexistente.");
        }
        verificarTransferencia(entity);
        String codigo = normalizarOperacion(referencia);
        if (codigo.equals(entity.getCodigoOperacion())) {
            return aPago(entity);
        }
        verificarPendiente(entity);
        entity.setCodigoOperacion(codigo);
        return aPago(entity);
    }

    @Transactional
    public Pago confirmarTransferencia(
            Long idPago, String operador, String referencia, BigDecimal montoAbonado) {
        if (operador == null || operador.isBlank() || operador.length() > 255) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        PagoEntity entity = buscarParaActualizar(idPago);
        verificarTransferencia(entity);
        String codigo = normalizarOperacion(referencia);
        if (!codigo.equals(entity.getCodigoOperacion())
                || montoAbonado == null
                || montoAbonado.compareTo(entity.getMonto()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operación y el importe abonado deben coincidir con el pago informado.");
        }
        if ("VALIDADO".equals(entity.getEstado())) {
            return aPago(entity);
        }
        verificarPendiente(entity);
        entity.conciliar(operador);
        try {
            pagos.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (PagoRepository.violaRestriccionUnica(ex, "uk_pago_operacion_transferencia")) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La operación ya fue confirmada para otro pedido.",
                        ex);
            }
            throw ex;
        }
        Pago pago = aPago(entity);
        resultados.registrar(pago);
        return pago;
    }

    private PagoEntity buscarParaActualizar(Long idPago) {
        return pagos.findForUpdate(idPago)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Pago inexistente."));
    }

    private void verificarTransferencia(PagoEntity entity) {
        if (entity.getMetodoTransferencia() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El pago no corresponde a una transferencia registrada.");
        }
    }

    private void verificarPendiente(PagoEntity entity) {
        if (!"PENDIENTE".equals(entity.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El pago ya tiene un resultado.");
        }
    }

    private String normalizarOperacion(String referencia) {
        String codigo = referencia == null ? "" : referencia.strip().toUpperCase(Locale.ROOT);
        if (!codigo.matches("[A-Z0-9][A-Z0-9-]{3,63}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ingresa el número de operación del comprobante.");
        }
        return codigo;
    }

    @Transactional
    public Pago cancelarDesdeOrden(Long idOrden, String idCliente, BigDecimal monto) {
        validarDatos(idOrden, idCliente, monto);
        PagoEntity entity =
                pagos.findByOrdenForUpdate(idOrden)
                        .orElseGet(
                                () ->
                                        pagos.saveAndFlush(
                                                new PagoEntity(
                                                        idOrden,
                                                        idCliente,
                                                        monto,
                                                        "CANCELADO",
                                                        Instant.now(),
                                                        null)));
        validarCorrespondencia(entity, idCliente, monto);
        if (entity.esPagoExterno()) {
            throw new IllegalStateException(
                    "La devolución se debe conciliar con la cuenta receptora.");
        }
        // A cancellation received before creation is a durable tombstone: replay can never charge
        // it.
        switch (entity.getEstado()) {
            case "VALIDADO" -> entity.setEstado("REEMBOLSADO");
            case "PENDIENTE", "FALLIDO" -> entity.setEstado("CANCELADO");
            case "CANCELADO", "REEMBOLSADO" -> {}
            default -> throw new IllegalStateException("Estado de pago desconocido.");
        }
        return aPago(pagos.save(entity));
    }

    private void validarDatos(Long idOrden, String cliente, BigDecimal monto) {
        if (idOrden == null
                || idOrden < 1
                || cliente == null
                || cliente.isBlank()
                || monto == null
                || monto.signum() <= 0
                || monto.stripTrailingZeros().scale() > 2
                || monto.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El pago requiere orden, cliente e importe positivo con dos decimales como"
                        + " máximo.");
        }
    }

    private void validarCorrespondencia(PagoEntity entity, String cliente, BigDecimal monto) {
        if (!entity.getIdCliente().equals(cliente) || entity.getMonto().compareTo(monto) != 0) {
            throw new IllegalArgumentException(
                    "El evento contradice el cliente o importe del pago existente.");
        }
    }

    @Transactional(readOnly = true)
    public Pago obtener(Long id) {
        return pagos.findById(id).map(this::aPago).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Pago> porOrden(Long idOrden) {
        return pagos.findAllByIdOrdenOrderByFechaCreacionAsc(idOrden).stream()
                .map(this::aPago)
                .toList();
    }

    Pago aPago(PagoEntity entity) {
        Transferencia transferencia =
                entity.getMetodoTransferencia() == null
                        ? null
                        : new Transferencia(
                                entity.getMetodoTransferencia(),
                                entity.getTitularDestino(),
                                entity.getCelularDestino(),
                                entity.getCodigoOperacion(),
                                "PEN");
        Yape yape =
                entity.esYapeEnLinea()
                        ? new Yape(
                                "PENDIENTE".equals(entity.getEstado())
                                        && entity.getYapeIntento() != null
                                        && entity.getYapeRechazoCodigo() == null,
                                entity.getYapeIntento(),
                                entity.getYapeOrdenProveedor(),
                                entity.getYapeRechazoCodigo())
                        : null;
        return new Pago(
                entity.getId(),
                entity.getIdOrden(),
                entity.getIdCliente(),
                entity.getMonto(),
                entity.getEstado(),
                entity.getFechaCreacion(),
                entity.getFechaAprobacion(),
                transferencia,
                yape);
    }

    public record Transferencia(
            String metodo, String titular, String celular, String codigoOperacion, String moneda) {}

    public record Yape(
            boolean enVerificacion, String referencia, String cargoId, String rechazoCodigo) {}

    public record Pago(
            Long id,
            Long idOrden,
            String idCliente,
            BigDecimal monto,
            String estado,
            Instant fechaCreacion,
            Instant fechaAprobacion,
            Transferencia transferencia,
            Yape yape) {
        public long revision() {
            return switch (estado) {
                case "VALIDADO", "FALLIDO" -> 1L;
                case "CANCELADO", "REEMBOLSADO" -> 2L;
                default -> throw new IllegalStateException("El pago todavía no tiene resultado.");
            };
        }
    }
}
