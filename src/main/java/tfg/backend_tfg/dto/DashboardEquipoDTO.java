package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEquipoDTO {
    // Contiene los totales de tareas y la lista de participación por estudiante
    private TareasEquipoDTO estadisticasTareas;

    // Contiene los totales de historias, puntos y participación por estudiante
    private HistoriasEquipoDTO estadisticasHistorias;

    private List<HistoriaDetalleDTO> detallesTaiga;
}
