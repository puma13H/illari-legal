package com.illarilegal.ordenlegal.repository;

import com.illarilegal.ordenlegal.entity.OrdenLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrdenLegalRepository extends JpaRepository<OrdenLegal, Long> {
    List<OrdenLegal> findByIdClienteOrderByIdDesc(Long idCliente);
}
