package tfg.backend_tfg.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardHomeDTO {

    private int totalCursosActivos;
    //Estudiante
    private int totalTareasPendientes;

    //Profesor
    private int totalCursos;
    private int totalEstudiantesAsignados;
    private int totalEquiposFormados;
    private List<CursoSummaryDTO> cursosRecientes;
    private List<EvaluacionCalendarDTO> evaluaciones;
}

