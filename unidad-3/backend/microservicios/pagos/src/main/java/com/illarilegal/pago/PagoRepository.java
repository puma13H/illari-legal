package com.illarilegal.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<PagoEntity, Long> {
    long countByEstado(String estado);
    @Query("select p.id from PagoEntity p where p.estado='PENDIENTE' and p.yapeCheckoutApi=true"
            + " and p.yapeIntento is not null and p.yapeRechazoCodigo is null and p.yapeAmbiente=:ambiente order by p.yapeUltimaRevision asc nulls first, p.id")
    List<Long> conciliables(@Param("ambiente") String ambiente, org.springframework.data.domain.Pageable limite);
    Optional<PagoEntity> findByIdOrden(Long idOrden);
    List<PagoEntity> findAllByIdOrdenOrderByFechaCreacionAsc(Long idOrden);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagoEntity p where p.idOrden = :idOrden")
    Optional<PagoEntity> findByOrdenForUpdate(@Param("idOrden") Long idOrden);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagoEntity p where p.id = :id")
    Optional<PagoEntity> findForUpdate(@Param("id") Long id);

    static boolean violaRestriccionUnica(DataIntegrityViolationException error, String restriccion) {
        if (!(error.getCause() instanceof ConstraintViolationException violacion)
                || !"23505".equals(violacion.getSQLState()) || violacion.getConstraintName() == null) {
            return false;
        }
        String nombre = violacion.getConstraintName().toLowerCase(Locale.ROOT);
        String indice = nombre.substring(nombre.lastIndexOf('.') + 1);
        return indice.equals(restriccion) || indice.startsWith(restriccion + "_index_");
    }
}
