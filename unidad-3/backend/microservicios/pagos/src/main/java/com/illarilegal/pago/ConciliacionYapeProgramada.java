package com.illarilegal.pago;

import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Solo consulta intentos existentes; nunca tokeniza ni emite un nuevo cargo. */
@Component
class ConciliacionYapeProgramada {
    private final PagoRepository payments;
    private final MercadoPagoYapeClient provider;
    private final CargoYapeService charges;
    ConciliacionYapeProgramada(PagoRepository payments, MercadoPagoYapeClient provider, CargoYapeService charges) {
        this.payments=payments; this.provider=provider; this.charges=charges;
    }
    @Scheduled(initialDelay=30000, fixedDelayString="${ILLARI_YAPE_CONCILIACION_MS:30000}")
    void revisar() {
        if (!provider.permiteConsulta()) return;
        for (Long id : payments.conciliables(provider.modo(), PageRequest.of(0,10))) {
            if (!provider.permiteConsulta()) break;
            try { charges.revisarPendiente(id); }
            catch (RuntimeException error) {
                LoggerFactory.getLogger(getClass()).info("yape_conciliacion_pendiente pagoId={}", id);
            }
        }
    }
}
