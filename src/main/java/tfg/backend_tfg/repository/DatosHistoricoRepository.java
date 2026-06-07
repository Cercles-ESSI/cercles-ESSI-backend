package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.DatosHistorico;

@Repository
public interface DatosHistoricoRepository extends JpaRepository<DatosHistorico, Integer> {
}
