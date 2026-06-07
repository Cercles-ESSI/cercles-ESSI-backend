package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tfg.backend_tfg.model.EvaluacionDetalle;
import java.util.List;

public interface EvaluacionDetalleRepository extends JpaRepository<EvaluacionDetalle, Integer> {
    List<EvaluacionDetalle> findByEvaluacionId(Integer evaluacionId);
    List<EvaluacionDetalle> findByEquipoId(Integer equipoId);
    List<EvaluacionDetalle> findByEquipoIdAndEvaluacionIdIn(Integer equipoId, List<Integer> evaluacionIds);
    boolean existsByEvaluacionIdAndEvaluadorId(Integer evaluacionId, Integer evaluadorId);
    List<EvaluacionDetalle> findByEquipoIdAndEvaluadoIdAndEvaluadorId(Integer equipoId, Integer evaluadoId, Integer evaluadorId);
}