package com.illarilegal.pago.repository;

import com.illarilegal.pago.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByIdOrdenOrderByIdDesc(Long idOrden);
}
