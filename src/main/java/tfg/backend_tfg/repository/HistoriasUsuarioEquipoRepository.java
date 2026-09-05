package tfg.backend_tfg.repository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.HistoriasUsuarioEquipo;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriasUsuarioEquipoRepository extends JpaRepository<HistoriasUsuarioEquipo, Integer> {
    List<HistoriasUsuarioEquipo> findByEquipo_Id(Integer equipoId);

    //Eliminar toodas las historias de usuario del equipo

    @Transactional
    @Modifying
    @Query("DELETE FROM HistoriasUsuarioEquipo h WHERE h.equipo.id = :equipoId")
    void deleteByEquipoId(@Param("equipoId") Integer equipoId);

    Optional<HistoriasUsuarioEquipo> findByIdHistoriaAndEquipoId(
            Integer idHistoria, Integer equipoId
    );
}
