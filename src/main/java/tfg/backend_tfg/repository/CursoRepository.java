package tfg.backend_tfg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.model.Profesor;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Integer> {
    Optional<Curso> findByNombreAsignaturaAndAñoInicioAndCuatrimestreAndActivo(
        String nombreAsignatura, int añoInicio, int cuatrimestre, boolean activo);

    Optional<Curso> findByNombreAsignaturaAndAñoInicioAndCuatrimestre(
        String nombreAsignatura, int añoInicio, int cuatrimestre);

    List<Curso> findAllByProfesoresContaining(Profesor profesor);

    // Total de cursos activos del estudiante
    @Query("SELECT COUNT(DISTINCT c) FROM Curso c JOIN c.equipos eq JOIN eq.estudiantes est WHERE est.id = :usuarioId AND c.activo = true")
    int countCursosActivosByEstudianteId(@Param("usuarioId") Integer usuarioId);

    // Trae los últimos 4 cursos creados (ordenados por ID de mayor a menor)
    List<Curso> findTop4ByProfesores_Profesor_CorreoOrderByIdDesc(String correo);

    //Total de cursos del profesor
    @Query("SELECT COUNT(c) FROM Curso c JOIN c.profesores pc WHERE pc.profesor.correo = :correo")
    int countCursosByProfesorCorreo(@Param("correo") String correo);

    //Total de cursos activos del profesor
    @Query("SELECT COUNT(c) FROM Curso c JOIN c.profesores pc WHERE pc.profesor.id = :usuarioId AND c.activo = true")
    int countCursosActivosByProfesorId(@Param("usuarioId") Integer usuarioId);

    // Total de estudiantes en los cursos de este profesor
    @Query("SELECT COALESCE(SUM(SIZE(c.estudiantes)), 0) FROM Curso c JOIN c.profesores pc WHERE pc.profesor.correo = :correo")
    int sumEstudiantesByProfesorCorreo(@Param("correo") String correo);

    // Total de equipos formados en los cursos de este profesor
    @Query("SELECT COALESCE(SUM(SIZE(c.equipos)), 0) FROM Curso c JOIN c.profesores pc WHERE pc.profesor.correo = :correo")
    int sumEquiposByProfesorCorreo(@Param("correo") String correo);
    
}
