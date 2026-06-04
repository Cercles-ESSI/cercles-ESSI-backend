package tfg.backend_tfg.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaDetalleDTO {
    private Integer id;
    private String titulo;
    private String estado;
    private Integer puntosEsfuerzo;
    private String sprint;


    private Map<String, Integer> tareasPorEstudiante = new HashMap<>();

    private int tareasSinAsignar;
    private int totalTareas;
    private int totalMiembros;
}
