package tfg.backend_tfg.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.HistoriasUsuarioEquipo;

import java.util.List;

@Repository
public interface HistoriasUsuarioEquipoRepository extends JpaRepository<HistoriasUsuarioEquipo, Integer> {
    List<HistoriasUsuarioEquipo> findByEquipo_Id(Integer equipoId);
}
