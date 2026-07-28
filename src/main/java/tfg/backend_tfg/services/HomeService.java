package tfg.backend_tfg.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tfg.backend_tfg.dto.CursoSummaryDTO;
import tfg.backend_tfg.dto.DashboardHomeDTO;
import tfg.backend_tfg.dto.EvaluacionCalendarDTO;
import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.model.Evaluacion;
import tfg.backend_tfg.model.Rol;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.repository.CursoRepository;
import tfg.backend_tfg.repository.EvaluacionRepository;
import tfg.backend_tfg.repository.TareasEquipoRepository;
import tfg.backend_tfg.repository.UsuarioRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeService {

    @Autowired
    private CursoRepository cursoRepository;
    private List<EvaluacionCalendarDTO> evaluaciones;

    @Autowired
    private TareasEquipoRepository tareasEquipoRepository;

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    //obtener resumen incial del estado de los cursos
    public DashboardHomeDTO obtenerResumenInicial(Integer usuarioId, String email, Rol rolUsuario){
        DashboardHomeDTO response = new DashboardHomeDTO();

        if(rolUsuario == Rol.Profesor){
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

            //Asignamos la lista al DTO principal
            response.setCursosRecientes(cursosDto);

            int todosCursos = cursoRepository.countCursosByProfesorCorreo(email);
            int cursosActivos = cursoRepository.countCursosActivosByProfesorId(usuarioId);
            int todosEstudiantes = cursoRepository.sumEstudiantesByProfesorCorreo(email);
            int equiposformados = cursoRepository.sumEquiposByProfesorCorreo(email);

            response.setTotalCursos(todosCursos);
            response.setTotalCursosActivos(cursosActivos);
            response.setTotalEquiposFormados(equiposformados);
            response.setTotalEstudiantesAsignados(todosEstudiantes);
        }else if(rolUsuario == Rol.Estudiante){
            int cursosActivos = cursoRepository.countCursosActivosByEstudianteId(usuarioId);
            int tareasPendientes = tareasEquipoRepository.countByEstudianteIdAndFechaCierreIsNull(usuarioId);
            response.setTotalCursosActivos(cursosActivos);
            response.setTotalTareasPendientes(tareasPendientes);
        }


        //Obtener evaluaciones para el calendario
        List<Evaluacion> evaluaciones = (rolUsuario == Rol.Profesor)
                ? evaluacionRepository.findEvaluacionesByCurso_Profesores_Profesor_Correo(email)
                : evaluacionRepository.findEvaluacionesByCurso_Estudiantes_Estudiante_Correo(email);

        evaluaciones.sort(Comparator.comparing(Evaluacion::getFechaInicio));
        Map<Integer, Integer> contadorPorCurso = new HashMap<>();

        List<EvaluacionCalendarDTO> evaluacionesDto = evaluaciones.stream()
                .map(ev -> {
                    // Calcular qué número de iteración le toca a esta evaluación
                    int numIteracion = 1;
                    String nombreAsig = "Sense assignatura";

                    if (ev.getCurso() != null) {
                        Integer cursoId = ev.getCurso().getId();
                        nombreAsig = ev.getCurso().getNombreAsignatura();

                        // Obtener el contador actual de ese curso y sumarle 1
                        numIteracion = contadorPorCurso.getOrDefault(cursoId, 0) + 1;
                        contadorPorCurso.put(cursoId, numIteracion);
                    }

                    return EvaluacionCalendarDTO.builder()
                            .id(ev.getId())
                            .fecha_inicio(ev.getFechaInicio())
                            .fecha_fin(ev.getFechaFin())
                            .nombreAsignatura(nombreAsig)
                            .numeroIteracion(numIteracion)
                            .build();
                }).collect(Collectors.toList());

        response.setEvaluaciones(evaluacionesDto);

        return response;
    }
}
