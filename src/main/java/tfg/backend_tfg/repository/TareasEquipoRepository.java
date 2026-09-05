package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.HistoriasUsuarioEquipo;
import tfg.backend_tfg.model.TareasEquipo;
import tfg.backend_tfg.model.Evaluacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TareasEquipoRepository extends JpaRepository<TareasEquipo, Integer> {
    List<TareasEquipo> findByEquipoId(Integer equipoId);

    //Eliminar todo registro del equipo
    @Transactional
    @Modifying
    @Query("DELETE FROM TareasEquipo t WHERE t.equipo.id = :equipoId")
    void deleteByEquipoId(@Param("equipoId") Integer equipoId);

    //Obtener las tareas pendientes de un estudiante en el Taiga
    // SELECT COUNT(*) FROM tareas_equipo WHERE estudiante_id = ? AND fecha_cierre IS NULL
    Integer countByEstudianteIdAndFechaCierreIsNull(Integer estudianteId);

    //Historico (revisar)
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

    Optional<TareasEquipo> findByIdTareaAndEquipoId(
            Integer idTarea, Integer equipoId
    );
}