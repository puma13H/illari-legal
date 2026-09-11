package com.illarilegal.serviciolegal.repository;

import com.illarilegal.serviciolegal.entity.ServicioLegal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicioLegalRepository extends JpaRepository<ServicioLegal, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
