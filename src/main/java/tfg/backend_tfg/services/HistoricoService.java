package tfg.backend_tfg.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tfg.backend_tfg.model.*;
import tfg.backend_tfg.repository.*;
import tfg.backend_tfg.dto.*;

import java.time.LocalDate;
import java.util.List;
public class HistoricoService {
    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private DatosHistoricoRepository datosHistoricoRepository;

    @Autowired
    private TaigaService taigaService;

    /**
     * Expresión CRON: "0 55 23 * * ?"
     * Significa: Segundo 0, Minuto 55, Hora 23, Todos los días del mes, Todos los meses.
     * Se ejecutará cada noche a las 23:55 PM de forma totalmente automática.
     */

    // @Scheduled(fixedRate = 60000) ejecuta cada 60seg.
    @Scheduled(cron = "0 59 23 * * ?")
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

                try {
                    // 3. Sincronizamos primero Taiga para tener los datos actualizados de última hora
                    taigaService.sincronizarTareas(equipo.getId());
                    taigaService.sincronizarHistorias(equipo.getId());

                    // 4. Llamamos a funciones estadísticas de Taiga
                    TareasEquipoDTO estadisticasTaiga = taigaService.calcularEstadisticasEquipoTareas(equipo.getId(), equipo.getTaigaProyecto());
                    HistoriasEquipoDTO estadisticasHistorias = taigaService.calcularEstadisticasHistorias(equipo.getId());

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

                        // 6. Construimos la entidad histórica (Snapshot)
                        DatosHistorico foto = DatosHistorico.builder()
                                .evaluacion(evaluacion)
                                .estudiante(estudiante)
                                .tareasCerradas(tareasEst.getTareasCerradas())
                                .tareasAbiertas(tareasEst.getTareasAbiertas())
                                .storyPointsCompletados(historiasEst.getPuntosEsfuerzo())
                                .commitsRealizados(0)
                                .lineasModificadas(0)
                                .build();

                        // 7. Guardamos en la base de datos relacional
                        datosHistoricoRepository.save(foto);
                    }
                } catch (Exception e) {
                    System.err.println("[CRON ERROR] Fallo al recolectar métricas del equipo " + equipo.getId() + ": " + e.getMessage());
                }
            }
        }
        System.out.println("[CRON CERCLES] Proceso de guardado histórico finalizado con éxito.");
    }
}
