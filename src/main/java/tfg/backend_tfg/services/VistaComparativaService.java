package tfg.backend_tfg.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tfg.backend_tfg.dto.ComparativaDTO;
import tfg.backend_tfg.dto.ComparativaDetalleDTO;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.MetricasEstudiante;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.HistoriasUsuarioEquipoRepository;
import tfg.backend_tfg.repository.MetricasEstudianteRepository;

import java.util.List;


@Service
public class VistaComparativaService {

    @Autowired
    private final HistoriasUsuarioEquipoRepository historiasRepo;

    @Autowired
    private final MetricasEstudianteRepository metricasRepo;

    @Autowired
    private final EquipoRepository equipoRepo;

    public VistaComparativaService(HistoriasUsuarioEquipoRepository historiasRepo, MetricasEstudianteRepository metricasRepo, EquipoRepository equipoRepo) {
        this.historiasRepo = historiasRepo;
        this.metricasRepo = metricasRepo;
        this.equipoRepo = equipoRepo;
    }


    public ComparativaDetalleDTO getMetricasEquipoCompleto(Integer cursoId) {
        List<Equipo> todosLosEquipos = equipoRepo.findByCursoId(cursoId);

        // Generar la lista de DTOs individuales
        List<ComparativaDTO> listaEquipos = todosLosEquipos.stream()
                .map(equipo -> getMetricasEquipo(equipo.getId()))
                .toList();

        // Calcular las métricas globales sumando los datos de la lista obtenida
        int commitsTotales = listaEquipos.stream()
                .mapToInt(e -> e.getGithub().getTotalCommits())
                .sum();

        int spCompletadosTotales = listaEquipos.stream()
                .mapToInt(e -> e.getTaiga().getCompletedStoryPoints())
                .sum();

        // Construir el contenedor final
        return ComparativaDetalleDTO.builder()
                .equipos(listaEquipos)
                .globales(ComparativaDetalleDTO.GlobalMetrics.builder()
                        .totalCommitsProyecto(commitsTotales)
                        .totalSpCompletadosProyecto(spCompletadosTotales)
                        .build())
                .build();
    }

    private String calcularBalanceInterno(List<MetricasEstudiante> metricasEquipo) {
        if (metricasEquipo == null || metricasEquipo.isEmpty()) return "SIN_DATOS";

        // Extraemos solo los commits de cada estudiante
        List <Integer> commitsPorEstudiante = metricasEquipo.stream()
                .map(MetricasEstudiante::getTotalCommits)
                .map(commits -> commits != null ? commits : 0)
                .toList();

        double media = commitsPorEstudiante.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        if (media == 0) return "SIN_ACTIVIDAD";

        // Calculamos la desviación estándar
        double varianza = commitsPorEstudiante.stream()
                .mapToDouble(commits -> Math.pow(commits - media, 2))
                .average().orElse(0.0);
        double desviacionEstandar = Math.sqrt(varianza);

        double coeficienteVariacion = (desviacionEstandar / media) * 100;


        if (coeficienteVariacion <= 20) return "ÓPTIMO";
        if (coeficienteVariacion <= 50) return "ACEPTABLE";
        return "DESEQUILIBRADO";
    }

    public ComparativaDTO getMetricasEquipo(Integer equipoId) {
        Equipo equipo = equipoRepo.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));


        Integer totalSp = historiasRepo.sumTotalStoryPointsByEquipoId(equipoId);
        Integer completedSp = historiasRepo.sumCompletedStoryPointsByEquipoId(equipoId);
        Integer totalCommits = metricasRepo.sumTotalCommitsByEquipoId(equipoId);
        Integer mergedPrs = metricasRepo.sumPullRequestsMergedByEquipoId(equipoId);

        int safeTotalSp = (totalSp != null) ? totalSp : 0;
        int safeCompletedSp = (completedSp != null) ? completedSp : 0;
        int safeCommits = (totalCommits != null) ? totalCommits : 0;
        int safePrs = (mergedPrs != null) ? mergedPrs : 0;

        double porcentaje = 0.0;
        if (safeTotalSp > 0) {
            porcentaje = ((double) safeCompletedSp / safeTotalSp) * 100.0;
            // Opcional: redondear a 2 decimales
            porcentaje = Math.round(porcentaje * 100.0) / 100.0;
        }

        List<MetricasEstudiante> metricasEstudiantes = metricasRepo.findByEquipoId(equipoId);
        String balanceCalculado = calcularBalanceInterno(metricasEstudiantes);



        return ComparativaDTO.builder()
                .equipoId(equipo.getId())
                .nombreEquipo(equipo.getNombre())
                .taiga(ComparativaDTO.TaigaMetrics.builder()
                        .completedStoryPoints(safeCompletedSp)
                        .totalStoryPoints(safeTotalSp)
                        .progressPercentage(porcentaje)
                        .build())
                .github(ComparativaDTO.GithubMetrics.builder()
                        .totalCommits(safeCommits)
                        .mergedPullRequests(safePrs)
                        .build())
                .balanceInterno(balanceCalculado)
                .build();
    }
}

