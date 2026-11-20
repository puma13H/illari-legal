package com.illarilegal.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {
    private final PagoService pagos;
    private final CargoYapeService yape;

    public PagoController(PagoService pagos, CargoYapeService yape) {
        this.pagos = pagos;
        this.yape = yape;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoService.Pago> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        PagoService.Pago pago = pagos.obtener(id);
        if (pago == null || (!esAdministrador(jwt) && !pago.idCliente().equals(jwt.getSubject()))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pago);
    }

    @GetMapping("/orden/{idOrden}")
    public ResponseEntity<List<PagoService.Pago>> porOrden(@PathVariable Long idOrden, @AuthenticationPrincipal Jwt jwt) {
        List<PagoService.Pago> pagosOrden = pagos.porOrden(idOrden);
        if (!esAdministrador(jwt) && pagosOrden.stream().anyMatch(pago -> !pago.idCliente().equals(jwt.getSubject()))) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pagosOrden);
    }

    @PutMapping("/{id}/operacion")
    public PagoService.Pago informarOperacion(@PathVariable Long id, @RequestBody OperacionInformada operacion,
            @AuthenticationPrincipal Jwt jwt) {
        return pagos.informarOperacion(id, jwt.getSubject(), operacion.codigoOperacion());
    }

    @PostMapping("/{id}/confirmacion")
    public PagoService.Pago confirmar(@PathVariable Long id, @RequestBody ConfirmacionTransferencia confirmacion,
            @AuthenticationPrincipal Jwt jwt) {
        return pagos.confirmarTransferencia(id, jwt.getSubject(), confirmacion.codigoOperacion(), confirmacion.montoAbonado());
    }

    public record OperacionInformada(String codigoOperacion) {}
    public record ConfirmacionTransferencia(String codigoOperacion, BigDecimal montoAbonado) {}

    @GetMapping("/{id}/yape")
    public CargoYapeService.Checkout checkoutYape(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return yape.checkout(id, jwt.getSubject());
    }

    @PostMapping("/{id}/yape")
    public PagoService.Pago cobrarYape(@PathVariable Long id, @RequestBody TokenYape token,
            @AuthenticationPrincipal Jwt jwt) {
        return yape.cobrar(id, jwt.getSubject(), jwt.getClaimAsString("email"), token.tokenId(), token.celular(), token.correo());
    }

    @PostMapping("/{id}/yape/conciliacion")
    public PagoService.Pago conciliarYape(@PathVariable Long id, @RequestBody ConciliacionYape cargo) {
        return yape.conciliar(id, cargo.ordenProveedorId());
    }

    public record TokenYape(String tokenId, String celular, String correo) {}
    public record ConciliacionYape(@JsonAlias("cargoId") String ordenProveedorId) {}

    private boolean esAdministrador(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Object roles = realmAccess == null ? null : realmAccess.get("roles");
        return roles instanceof List<?> lista
                && lista.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.toString()));
    }
}
