package tfg.backend_tfg.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tfg.backend_tfg.dto.CursoSummaryDTO;
import tfg.backend_tfg.dto.DashboardHomeDTO;
import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.repository.CursoRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HomeService {

    @Autowired
    private CursoRepository cursoRepository;

    //obtener resumen incial del estado de los cursos
    public DashboardHomeDTO obtenerResumenInicial(String email){
        DashboardHomeDTO response = new DashboardHomeDTO();
        List<Curso> ultimosCursos = cursoRepository.findTop4ByProfesores_Profesor_CorreoOrderByIdDesc(email);

        List<CursoSummaryDTO> cursosDto = ultimosCursos.stream()
                .map(curso -> CursoSummaryDTO.builder()
                        .id(curso.getId())
                        .nombreAsignatura(curso.getNombreAsignatura())
                        .añoInicio(curso.getAñoInicio())
                        .cuatrimestre(curso.getCuatrimestre())
                        .activo(curso.isActivo())
                        .numeroEstudiantes(curso.getEstudiantes() != null ? curso.getEstudiantes().size() : 0)
                        .numeroEquipos(curso.getEquipos() != null ? curso.getEquipos().size() : 0)
                        .build()
                )
                .collect(Collectors.toList());

        // 3. Asignamos la lista al DTO principal
        response.setCursosRecientes(cursosDto);

        int todosCursos = cursoRepository.countCursosByProfesorCorreo(email);
        int cursosActivos = cursoRepository.countCursosActivosByProfesorCorreo(email);
        int todosEstudiantes = cursoRepository.sumEstudiantesByProfesorCorreo(email);
        int equiposformados = cursoRepository.sumEquiposByProfesorCorreo(email);

        response.setTotalCursos(todosCursos);
        response.setTotalCursosActivos(cursosActivos);
        response.setTotalEquiposFormados(equiposformados);
        response.setTotalEstudiantesAsignados(todosEstudiantes);

        return response;
    }
}
