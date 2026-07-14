package tfg.backend_tfg.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoSummaryDTO {
    private int id;
    private String nombreAsignatura;
    private int añoInicio;
    private int cuatrimestre;
    private boolean activo;
    private String gestionado;
    private int numeroEstudiantes;
    private int numeroEquipos;
    private int numeroEstudiantesSinEquipo;

    public CursoSummaryDTO(int id, String nombreAsignatura, int añoInicio, int cuatrimestre, boolean activo, int numeroEstudiantes, int numeroEquipos,
    int numeroEstudiantesSinEquipo, String gestionado) {
        this.id = id;
        this.nombreAsignatura = nombreAsignatura;
        this.añoInicio = añoInicio;
        this.cuatrimestre = cuatrimestre;
        this.activo = activo;
        this.gestionado = gestionado;
        this.numeroEstudiantes = numeroEstudiantes;
        this.numeroEquipos = numeroEquipos;
        this.numeroEstudiantesSinEquipo = numeroEstudiantesSinEquipo;
    }
}
