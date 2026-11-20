package com.illarilegal.pago;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
class CargoYapeService {
    private final PagoRepository repositorio;
    private final PagoService pagos;
    private final ResultadoPagoOutbox outbox;
    private final MercadoPagoYapeClient proveedor;
    private final TransactionTemplate transaccion;

    CargoYapeService(
            PagoRepository repositorio,
            PagoService pagos,
            ResultadoPagoOutbox outbox,
            MercadoPagoYapeClient proveedor,
            PlatformTransactionManager manager) {
        this.repositorio = repositorio;
        this.pagos = pagos;
        this.outbox = outbox;
        this.proveedor = proveedor;
        transaccion = new TransactionTemplate(manager);
        transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    Checkout checkout(Long id, String cliente) {
        PagoService.Pago pago = pagoDelCliente(id, cliente);
        proveedor.verificarHabilitado();
        verificarImporteAdmitido(pago);
        proveedor.verificarMonto(pago.monto());
        if (!"PENDIENTE".equals(pago.estado()) || pago.yape().enVerificacion()) {
            throw cargoEnVerificacion();
        }
        long centimos = pago.monto().movePointRight(2).longValueExact();
        return new Checkout(proveedor.llavePublica(), centimos, "PEN", proveedor.modo());
    }

    PagoService.Pago cobrar(
            Long id,
            String cliente,
            String email,
            String token,
            String celular,
            String correoPagador) {
        PagoService.Pago pago = pagoDelCliente(id, cliente);
        proveedor.verificarHabilitado();
        if ("VALIDADO".equals(pago.estado())) {
            return pago;
        }
        verificarImporteAdmitido(pago);
        proveedor.verificarMonto(pago.monto());
        if (!"PENDIENTE".equals(pago.estado()) || pago.yape().enVerificacion()) {
            throw cargoEnVerificacion();
        }
        if (token == null || !token.matches("[a-f0-9]{32}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Usa un token Yape generado por Mercado Pago.");
        }
        String patronCelular = proveedor.esPrueba() ? "11111111[1-8]" : "9[0-9]{8}";
        if (celular == null || !celular.matches(patronCelular)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    proveedor.esPrueba()
                            ? "Usa un número de prueba de Yape."
                            : "Ingresa un celular peruano de nueve dígitos.");
        }
        String correoCuenta = correoPagador == null ? email : correoPagador.strip();
        String correo = proveedor.esPrueba() ? "test_payer@example.com" : correoCuenta;
        if (correo == null
                || correo.length() > 254
                || !correo.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La cuenta necesita un correo válido para el cargo.");
        }
        proveedor.verificarVendedor();
        Intento intento = reservar(id, cliente, huella(token));
        if ("VALIDADO".equals(intento.pago().estado())) {
            return intento.pago();
        }
        try {
            JsonNode orden =
                    proveedor.cobrar(
                            token,
                            correo,
                            celular,
                            intento.pago().monto().toPlainString(),
                            intento.referencia());
            return confirmar(intento, orden, null);
        } catch (MercadoPagoYapeClient.CargoDenegado ex) {
            throw liberarRechazo(intento);
        } catch (MercadoPagoYapeClient.ImporteRechazado ex) {
            transaccion.executeWithoutResult(
                    status -> {
                        PagoEntity entity = buscar(id);
                        if ("PENDIENTE".equals(entity.getEstado())
                                && intento.referencia().equals(entity.getYapeIntento())) {
                            entity.registrarRechazoImporte(ex.requestId());
                            repositorio.saveAndFlush(entity);
                        }
                    });
            throw importeNoAdmitido();
        }
    }

    private void verificarImporteAdmitido(PagoService.Pago pago) {
        if (pago.yape() != null && "invalid_transaction_amount".equals(pago.yape().rechazoCodigo()))
            throw importeNoAdmitido();
    }

    private ResponseStatusException importeNoAdmitido() {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Mercado Pago rechazó el importe de este pedido. No se generó una orden de cobro;"
                        + " solicita revisar el importe antes de reintentar.");
    }

    PagoService.Pago conciliar(Long id, String ordenId) {
        proveedor.verificarHabilitado();
        if (!proveedor.esOrdenDelEntorno(ordenId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingresa un ID de orden de Mercado Pago del entorno configurado.");
        }
        Intento intento =
                transaccion.execute(
                        status -> {
                            PagoEntity pago = buscar(id);
                            if (pago.getYapeIntento() == null
                                    || !proveedor.modo().equals(pago.getYapeAmbiente())) {
                                throw new ResponseStatusException(
                                        HttpStatus.CONFLICT,
                                        "No hay un intento de Yape para este entorno.");
                            }
                            return intento(pago);
                        });
        if (ordenId.equals(intento.pago().yape().cargoId())) {
            return intento.pago();
        }
        proveedor.verificarVendedor();
        return confirmar(intento, proveedor.consultar(ordenId), ordenId);
    }

    void revisarPendiente(Long id) {
        record Pendiente(String referencia, java.time.Instant fecha) {}
        Pendiente pending = transaccion.execute(status -> {
            PagoEntity payment = buscar(id);
            if (!"PENDIENTE".equals(payment.getEstado()) || payment.getYapeIntento() == null
                    || payment.getYapeRechazoCodigo() != null || !proveedor.modo().equals(payment.getYapeAmbiente())) return null;
            payment.registrarRevisionYape();
            repositorio.saveAndFlush(payment);
            return new Pendiente(payment.getYapeIntento(), payment.getYapeFechaIntento());
        });
        if (pending != null) proveedor.buscarOrden(pending.referencia(), pending.fecha())
                .ifPresent(orderId -> conciliar(id, orderId));
    }

    private PagoService.Pago pagoDelCliente(Long id, String cliente) {
        return repositorio
                .findById(id)
                .filter(pago -> pago.esYapeCheckoutApi() && pago.getIdCliente().equals(cliente))
                .map(pagos::aPago)
                .orElseThrow(CargoYapeService::pagoNoEncontrado);
    }

    private Intento reservar(Long id, String cliente, String tokenHuella) {
        try {
            return transaccion.execute(
                    status -> {
                        PagoEntity pago = buscar(id);
                        if (!pago.getIdCliente().equals(cliente)) {
                            throw pagoNoEncontrado();
                        }
                        if ("VALIDADO".equals(pago.getEstado())) {
                            return intento(pago);
                        }
                        if (!"PENDIENTE".equals(pago.getEstado())
                                || pago.getYapeIntento() != null) {
                            throw cargoEnVerificacion();
                        }
                        pago.reservarCargoYape(
                                UUID.randomUUID().toString(), proveedor.modo(), tokenHuella);
                        repositorio.saveAndFlush(pago);
                        return intento(pago);
                    });
        } catch (DataIntegrityViolationException ex) {
            if (PagoRepository.violaRestriccionUnica(ex, "uk_pago_yape_token")) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El token Yape ya está asociado a otro pedido.");
            }
            throw ex;
        }
    }

    private PagoService.Pago confirmar(Intento intento, JsonNode orden, String idConsultado) {
        String ordenId = verificarOrden(intento, orden, idConsultado);
        return transaccion.execute(
                status -> {
                    PagoEntity pago = buscar(intento.pago().id());
                    if (!intento.referencia().equals(pago.getYapeIntento())) {
                        throw cargoEnVerificacion();
                    }
                    if (ordenId.equals(pago.getYapeOrdenProveedor())) {
                        return pagos.aPago(pago);
                    }
                    if (!"PENDIENTE".equals(pago.getEstado())) {
                        throw cargoEnVerificacion();
                    }
                    pago.confirmarCargoYape(ordenId);
                    repositorio.saveAndFlush(pago);
                    PagoService.Pago confirmado = pagos.aPago(pago);
                    outbox.registrar(confirmado);
                    return confirmado;
                });
    }

    private String verificarOrden(Intento intento, JsonNode orden, String idConsultado) {
        if (orden == null) {
            throw MercadoPagoYapeClient.resultadoDesconocido();
        }
        String id = orden.path("id").asText();
        BigDecimal monto = intento.pago().monto();
        JsonNode transacciones = orden.path("transactions").path("payments");
        if (!proveedor.esOrdenDelEntorno(id)
                || (idConsultado != null && !idConsultado.equals(id))
                || !intento.referencia().equals(orden.path("external_reference").asText())
                || !"online".equals(orden.path("type").asText())
                || !"automatic".equals(orden.path("processing_mode").asText())
                || !"PEN".equals(orden.path("currency").asText())
                || !coincideMonto(orden.path("total_amount"), monto)
                || !transacciones.isArray()
                || transacciones.size() != 1) {
            throw MercadoPagoYapeClient.resultadoDesconocido();
        }
        JsonNode pago = transacciones.get(0);
        if (pago.path("id").asText().isBlank()
                || !"yape".equals(pago.path("payment_method").path("id").asText())
                || !coincideMonto(pago.path("amount"), monto)) {
            throw MercadoPagoYapeClient.resultadoDesconocido();
        }
        if ("failed".equals(orden.path("status").asText())
                && "failed".equals(pago.path("status").asText())) {
            throw liberarRechazo(intento);
        }
        if (!acreditado(orden)
                || !acreditado(pago)
                || !coincideMonto(orden.path("total_paid_amount"), monto)
                || !coincideMonto(pago.path("paid_amount"), monto)) {
            throw MercadoPagoYapeClient.resultadoDesconocido();
        }
        return id;
    }

    private ResponseStatusException liberarRechazo(Intento intento) {
        transaccion.executeWithoutResult(
                status -> {
                    PagoEntity pago = buscar(intento.pago().id());
                    if ("PENDIENTE".equals(pago.getEstado())
                            && intento.referencia().equals(pago.getYapeIntento())) {
                        pago.liberarCargoRechazado();
                    }
                });
        return new ResponseStatusException(
                HttpStatus.PAYMENT_REQUIRED,
                "Yape rechazó el cargo. Genera un nuevo código de aprobación para reintentar.");
    }

    private static boolean acreditado(JsonNode nodo) {
        return "processed".equals(nodo.path("status").asText())
                && "accredited".equals(nodo.path("status_detail").asText());
    }

    private static boolean coincideMonto(JsonNode valor, BigDecimal monto) {
        String decimal = valor.asText();
        return decimal.matches("[0-9]{1,10}(\\.[0-9]{1,2})?")
                && new BigDecimal(decimal).compareTo(monto) == 0;
    }

    private PagoEntity buscar(Long id) {
        return repositorio
                .findForUpdate(id)
                .filter(PagoEntity::esYapeCheckoutApi)
                .orElseThrow(CargoYapeService::pagoNoEncontrado);
    }

    private Intento intento(PagoEntity pago) {
        return new Intento(pagos.aPago(pago), pago.getYapeIntento());
    }

    private static String huella(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo de hash no disponible.", ex);
        }
    }

    private static ResponseStatusException pagoNoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago Yape inexistente.");
    }

    private static ResponseStatusException cargoEnVerificacion() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "El pago ya tiene un resultado o un cargo en verificación.");
    }

    record Checkout(String llavePublica, long montoCentimos, String moneda, String entorno) {}

    private record Intento(PagoService.Pago pago, String referencia) {}
}
