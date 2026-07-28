package tfg.backend_tfg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tfg.backend_tfg.model.Evaluacion;

import java.time.LocalDate;
import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Integer> {
    List<Evaluacion> findByCursoId(Integer cursoId);

    @Query("SELECT e FROM Evaluacion e WHERE e.curso.id IN (SELECT ec.curso.id FROM Equipo ec WHERE ec.id = :equipoId)")
    List<Evaluacion> findByEquipoId(@Param("equipoId") Integer equipoId);

    @Query("SELECT COUNT(e) FROM Evaluacion e WHERE e.curso.id = :cursoId")
    Integer countByCursoId(@Param("cursoId") Integer cursoId);

    @Query("SELECT e.id FROM Evaluacion e WHERE e.curso.id = :cursoId")
    List<Integer> findIdsByCursoId(@Param("cursoId") Integer cursoId);

    List<Evaluacion> findByFechaFin(LocalDate fechaFin);

    //Encontrar todas las evaluaciones de un professor con su correo
    List<Evaluacion> findEvaluacionesByCurso_Profesores_Profesor_Correo(String correo);

    //Encontrar todas las evaluaciones de un estudiante con su correo
    List<Evaluacion> findEvaluacionesByCurso_Estudiantes_Estudiante_Correo(String correo);


}