package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ComparativaDTO {
    private Integer equipoId;
    private String nombreEquipo;

    // Objeto anidado para agrupar las métricas de gestión
    private TaigaMetrics taiga;

    // Objeto anidado para agrupar las métricas de desarrollo
    private GithubMetrics github;

    // Etiqueta del balance
    private String balanceInterno;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaigaMetrics {
        private Integer completedStoryPoints;
        private Integer totalStoryPoints;
        private Double progressPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GithubMetrics {
        private Integer totalCommits;
        private Integer mergedPullRequests;
    }

}

