package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tfg.backend_tfg.model.MetricasEstudiante;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricasEstudianteRepository extends JpaRepository<MetricasEstudiante, Integer> {

    Optional<MetricasEstudiante> findByEstudianteIdAndEquipoId(Integer estudianteId, Integer equipoId);
    List<MetricasEstudiante> findByEquipoId(Integer equipoId);

    //Eliminar todo registro del equipo
    @Transactional
    @Modifying
    @Query("DELETE FROM MetricasEstudiante m WHERE m.equipo.id = :equipoId")
    void deleteByEquipoId(@Param("equipoId") Integer equipoId);

    // Total de commits de un equipo
    @Query("SELECT SUM(m.totalCommits) FROM MetricasEstudiante m WHERE m.equipo.id = :equipoId")
    Integer sumTotalCommitsByEquipoId(@Param("equipoId") Integer equipoId);

    // Total de PRs fusionados de un equipo
    @Query("SELECT SUM(m.pullRequestsMerged) FROM MetricasEstudiante m WHERE m.equipo.id = :equipoId")
    Integer sumPullRequestsMergedByEquipoId(@Param("equipoId") Integer equipoId);

}
