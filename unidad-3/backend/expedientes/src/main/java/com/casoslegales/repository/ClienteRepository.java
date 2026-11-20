package com.casoslegales.repository;

import com.casoslegales.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Cliente c where c.id=:id")
    Optional<Cliente> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<Cliente> findByUserId(Long userId);
    Optional<Cliente> findByEmail(String email);
}
