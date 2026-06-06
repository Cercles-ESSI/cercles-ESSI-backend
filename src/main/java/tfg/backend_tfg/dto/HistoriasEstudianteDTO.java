package tfg.backend_tfg.dto;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class HistoriasEstudianteDTO {
    private Integer estudianteId;
    private String nombreEstudiante;
    private int totalHistoriasParticipadas;
    private double porcentajeHistorias;
    private int historiasAbiertas;
    private int historiasCerradas;
    private int puntosEsfuerzo;

}
