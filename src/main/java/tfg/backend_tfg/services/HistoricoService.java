package tfg.backend_tfg.services;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tfg.backend_tfg.model.*;
import tfg.backend_tfg.repository.*;
import tfg.backend_tfg.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import tfg.backend_tfg.security.TokenEncrypter;

@Service
public class HistoricoService {
    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private DatosHistoricoRepository datosHistoricoRepository;

    @Autowired
    private MetricasEstudianteRepository metricasEstudianteRepository;

    @Autowired
    private TaigaService taigaService;

    @Autowired
    private GithubService githubService;

    @Autowired
    private TareasEquipoRepository tareasEquipoRepository;

    private final TokenEncrypter tokenEncrypter;

    public HistoricoService(TokenEncrypter tokenEncrypter) {
        this.tokenEncrypter = tokenEncrypter;
    }

    /**
     * Expresión CRON: "0 59 23 * * ?"
     * Significa: Segundo 0, Minuto 59, Hora 23, Todos los días del mes, Todos los meses.
     * Se ejecutará cada noche a las 23:59 PM de forma totalmente automática.
     */

    /*@Scheduled(cron = "0 59 23 * * ?")*/
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void recolectarSnapshotsAutomaticos() {
        LocalDate hoy = LocalDate.now();
        System.out.println("[CRON CERCLES] Iniciando comprobación de evaluaciones para el día: " + hoy);

        // 1. Buscamos si hoy finaliza alguna evaluación en toda la base de datos
        List<Evaluacion> evaluacionesQueTerminanHoy = evaluacionRepository.findByFechaFin(hoy);

        if (evaluacionesQueTerminanHoy.isEmpty()) {
            System.out.println("[CRON CERCLES] No hay ninguna evaluación que termine hoy. Tarea finalizada.");
            return;
        }

        // 2. Si hay evaluaciones que terminan hoy, procesamos los equipos implicados
        for (Evaluacion evaluacion : evaluacionesQueTerminanHoy) {
            Integer cursoId = evaluacion.getCurso().getId();

            // Buscamos todos los equipos que pertenecen a ese curso
            List<Equipo> equiposDelCurso = equipoRepository.findByCursoId(cursoId);

            for (Equipo equipo : equiposDelCurso) {
                System.out.println("[CRON CERCLES] Recolectando foto histórica para el equipo: " + equipo.getNombre() + " en la evaluación ID: " + evaluacion.getId());

                if(evaluacion.getCurso().getGestionTareas().equals("Taiga") && equipo.getTaigaProyectoId() == null){
                    System.out.println("[CRON AVISO] El equipo " + equipo.getNombre() + " no tiene Taiga configurado (ID nulo). Se salta este equipo.");
                    continue;
                }else if(equipo.getGitOrganizacion() == null){
                    System.out.println("[CRON AVISO] El equipo " + equipo.getNombre() + " no tiene GitHub configurado (organizacion nulo). Se salta este equipo.");
                    continue;
                }


                try {
                    // 3. Sincronizamos primero Taiga i GitHub para tener los datos actualizados de última hora
                    taigaService.sincronizarTareas(equipo.getId(),false);
                    taigaService.sincronizarHistorias(equipo.getId(),false);

                    SyncGitHub(equipo,evaluacion.getCurso().getTokenGithubAsignatura());

                    // 4. Llamamos a funciones estadísticas de Taiga
                   TareasEquipoDTO estadisticasTaiga = taigaService.calcularEstadisticasEquipoTareas(equipo.getId(), equipo.getTaigaProyecto(), "hola", 11);
                    HistoriasEquipoDTO estadisticasHistorias = taigaService.calcularEstadisticasHistorias(equipo.getId(), "hola", 11);



                    // 5. Sacamos la fotografía miembro por miembro del equipo
                    for (Estudiante estudiante : equipo.getEstudiantes()) {

                        // Extraemos las métricas de tareas de este alumno
                        TareasEstudianteDTO tareasEst = estadisticasTaiga.getMetricasEstudiantes().stream()
                                .filter(t -> t.getEstudianteId().equals(estudiante.getId()))
                                .findFirst()
                                .orElse(new TareasEstudianteDTO());

                        // Extraemos las métricas de puntos de esfuerzo de este alumno
                        HistoriasEstudianteDTO historiasEst = estadisticasHistorias.getMetricasEstudiantes().stream()
                                .filter(h -> h.getEstudianteId().equals(estudiante.getId()))
                                .findFirst()
                                .orElse(new HistoriasEstudianteDTO());

                        MetricasEstudiante metricasGit = metricasEstudianteRepository
                                .findByEstudianteIdAndEquipoId(estudiante.getId(), equipo.getId())
                                .orElse(new MetricasEstudiante());
                        int commits = metricasGit.getTotalCommits() != null ? metricasGit.getTotalCommits() : 0;
                        int lineasAñadidas = metricasGit.getLinesAdded() != null ? metricasGit.getLinesAdded() : 0;
                        int lineasBorradas = metricasGit.getLinesRemoved() != null ? metricasGit.getLinesRemoved() : 0;

                        int lineasModificadas = lineasAñadidas + lineasBorradas;

                        // 6. Construimos la entidad histórica (Snapshot)
                        DatosHistorico foto = DatosHistorico.builder()
                                .evaluacion(evaluacion)
                                .estudiante(estudiante)
                                .tareasCerradas(tareasEst.getTareasCerradas())
                                .tareasAbiertas(tareasEst.getTareasAbiertas())
                                .storyPointsCompletados(historiasEst.getPuntosEsfuerzo())
                                .commitsRealizados(commits)
                                .lineasModificadas(lineasModificadas)
                                .build();

                        // 7. Guardamos en la base de datos relacional
                        datosHistoricoRepository.save(foto);
                    }

                    // PASO 8. Vinculamos las tareas a la evaluación
                    // Fijamos la hora al último segundo del día para incluir todas las tareas creadas hoy
                    LocalDateTime fechaFinEvaluacion = evaluacion.getFechaFin().atTime(23, 59, 59);

                    tareasEquipoRepository.vincularTareasAEvaluacion(
                            evaluacion,
                            equipo.getId(),
                            fechaFinEvaluacion
                    );

                    System.out.println("[CRON CERCLES] Tareas del equipo " + equipo.getId() + " vinculadas con éxito a la evaluación " + evaluacion.getId());

                } catch (Exception e) {
                    System.err.println("[CRON ERROR] Fallo al recolectar métricas del equipo " + equipo.getId() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("[CRON CERCLES] Proceso de guardado histórico finalizado con éxito.");
    }

    public void SyncGitHub(Equipo equipo, String tokenGithub){
        List<Integer> estudiantesIds = equipo.getEstudiantes().stream()
                .map(Estudiante::getId)
                .toList();

        List<String> usuariosGit = equipo.getEstudiantes().stream()
                .map(Estudiante::getGitUsername)
                .filter(username -> username != null && !username.trim().isEmpty())
                .toList();

        String organizacionGit = equipo.getGitOrganizacion();

        String tokenDescifrado = null;
        try {
            if (tokenGithub != null) {
                tokenDescifrado = tokenEncrypter.decrypt(tokenGithub);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el token del curso.", e);
        }

        if (organizacionGit != null && tokenDescifrado != null && !usuariosGit.isEmpty()) {
            try {
                System.out.println("[CRON CERCLES] Sincronizando GitHub para el equipo: " + equipo.getNombre());

                githubService.obtenerMetricasOrganizacion(
                        organizacionGit,
                        usuariosGit,
                        tokenDescifrado,
                        estudiantesIds,
                        false,
                        equipo.getId(),
                        false
                );
            } catch (Exception e) {
                System.err.println("[CRON ERROR] Fallo al sincronizar GitHub del equipo " + equipo.getId() + ": " + e.getMessage());
            }
        } else {
            System.out.println("[CRON AVISO] El equipo " + equipo.getNombre() + " no tiene GitHub configurado. Se usarán los datos cacheados.");
        }
    }


    public List<HistoricoFrontendDTO> obtenerDatosDashboard(Integer equipoId) {
        // 1. Sacamos los IDs de los estudiantes del equipo
        Equipo equipo = equipoRepository.findById(equipoId).orElseThrow();
        List<Integer> idsEstudiantes = equipo.getEstudiantes().stream()
                .map(Estudiante::getId)
                .toList();

        // 2. Buscamos todo el historial de esos estudiantes en la BD
        List<DatosHistorico> registros = datosHistoricoRepository.findByEstudianteIdIn(idsEstudiantes);

        // 3. Agrupamos los registros por Evaluación (la "fotografía")
        Map<Evaluacion, List<DatosHistorico>> registrosPorEvaluacion = registros.stream()
                .collect(Collectors.groupingBy(DatosHistorico::getEvaluacion));

        // 4. Convertimos cada grupo en un DTO para el frontend
        List<HistoricoFrontendDTO> dashboardData = new ArrayList<>();

        for (Map.Entry<Evaluacion, List<DatosHistorico>> entry : registrosPorEvaluacion.entrySet()) {
            Evaluacion evaluacion = entry.getKey();
            List<DatosHistorico> historialEvaluacion = entry.getValue();

            Map<String, Integer> tareasCerradas = new HashMap<>();
            Map<String, Integer> storyPoints = new HashMap<>();
            Map<String, Integer> commits = new HashMap<>();
            Map<String, Integer> lineas = new HashMap<>();

            for (DatosHistorico registro : historialEvaluacion) {

                String nombreEstudiante = registro.getEstudiante().getNombre();

                tareasCerradas.put(nombreEstudiante, registro.getTareasCerradas());
                storyPoints.put(nombreEstudiante, registro.getStoryPointsCompletados());
                commits.put(nombreEstudiante, registro.getCommitsRealizados());
                lineas.put(nombreEstudiante, registro.getLineasModificadas());
            }

            // Construimos el objeto que Recharts en React entiende perfectamente
            HistoricoFrontendDTO dto = HistoricoFrontendDTO.builder()
                    .iteracion("Avaluació " + evaluacion.getId())
                    .fechaGuardado(evaluacion.getFechaFin().toString())
                    .tareasCerradas(tareasCerradas)
                    .storyPoints(storyPoints)
                    .commitsRealizados(commits)
                    .lineasModificadas(lineas)
                    .build();

            dashboardData.add(dto);
        }

        // 5. Ordenamos por fecha para que el gráfico vaya de izquierda (antiguo) a derecha (nuevo)
        dashboardData.sort(Comparator.comparing(HistoricoFrontendDTO::getFechaGuardado));

        return dashboardData;
    }
}