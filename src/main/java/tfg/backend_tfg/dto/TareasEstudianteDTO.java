package tfg.backend_tfg.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TareasEstudianteDTO {
    private Integer estudianteId;
    private String nombreEstudiante;

    // Filas de la tabla para este estudiante correspondientes a Tareas
    private int totalTareas;
    private double porcentajeTareas;
    private int tareasAbiertas;
    private int tareasCerradas;
}
