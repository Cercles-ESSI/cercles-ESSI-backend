package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.DatosHistorico;

import java.util.List;

@Repository
public interface DatosHistoricoRepository extends JpaRepository<DatosHistorico, Integer> {
    List<DatosHistorico> findByEstudianteIdIn(List<Integer> estudianteIds);
}
