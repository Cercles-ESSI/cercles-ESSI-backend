package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparativaDetalleDTO {

    private List<ComparativaDTO> equipos;
    private GlobalMetrics globales;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalMetrics {
        private Integer totalCommitsProyecto;
        private Integer totalSpCompletadosProyecto;
        private Integer totalPullRequestsProyecto;
    }
}

