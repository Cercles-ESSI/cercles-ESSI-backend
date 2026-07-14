package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tfg.backend_tfg.model.TareasEquipo;
import tfg.backend_tfg.model.Evaluacion;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TareasEquipoRepository extends JpaRepository<TareasEquipo, Integer> {
    List<TareasEquipo> findByEquipoId(Integer equipoId);

    @Modifying
    @Transactional
    @Query("UPDATE TareasEquipo t SET t.evaluacion = :evaluacion " +
            "WHERE t.equipo.id = :equipoId " +
            "AND t.evaluacion IS NULL " +
            "AND t.fechaCreacion <= :fechaFin")
    void vincularTareasAEvaluacion(
            @Param("evaluacion") Evaluacion evaluacion,
            @Param("equipoId") Integer equipoId,
            @Param("fechaFin") LocalDateTime fechaFin
    );
    List<TareasEquipo> findByEquipoIdAndEvaluacionId(Integer equipoId, Integer evaluacionId);
}