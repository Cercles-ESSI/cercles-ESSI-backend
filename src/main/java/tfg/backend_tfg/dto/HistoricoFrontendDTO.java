package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoFrontendDTO {


    private String iteracion;
    private String fechaGuardado;

    private Map<String, Integer> tareasCerradas;
    private Map<String, Integer> storyPoints;
    private Map<String, Integer> commitsRealizados;
    private Map<String, Integer> lineasModificadas;
}