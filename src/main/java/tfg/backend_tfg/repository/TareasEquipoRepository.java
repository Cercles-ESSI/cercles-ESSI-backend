package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.TareasEquipo;
import java.util.List;

@Repository
public interface TareasEquipoRepository extends JpaRepository<TareasEquipo, Integer> {
    List<TareasEquipo> findByEquipoId(Integer equipoId);
}