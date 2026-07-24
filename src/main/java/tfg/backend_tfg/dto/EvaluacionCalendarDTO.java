package tfg.backend_tfg.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EvaluacionCalendarDTO {
    private Integer id;
    private LocalDate fecha_inicio;
    private LocalDate fecha_fin;
    private Integer curso_id;
    private String nombreAsignatura;
}
