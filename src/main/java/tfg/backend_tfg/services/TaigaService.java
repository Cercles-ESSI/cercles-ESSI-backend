package tfg.backend_tfg.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import tfg.backend_tfg.dto.*;
import tfg.backend_tfg.model.*;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.TareasEquipoRepository;
import tfg.backend_tfg.repository.UsuarioRepository;
import tfg.backend_tfg.repository.HistoriasUsuarioEquipoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaigaService {

    @Value("${taiga.api.url}")
    private String taigaApiBaseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private TareasEquipoRepository tareasRepository;

    @Autowired
    private HistoriasUsuarioEquipoRepository historiasRepository;

    public TaigaService() {
        this.restTemplate = new RestTemplate();
    }

    private static final Logger logger = LoggerFactory.getLogger(TaigaService.class);

    //1. validar la org de un equipo
    public Map<String, Boolean> validarProyecto(Integer profesorId, List<Integer> miembrosIds, String profesorTaiga, String proyectoUrl) {

        String prefijoProyecto = "project/";

        String proyecto;
        if (proyectoUrl.contains(prefijoProyecto)) {
            // Cortamos todo lo que haya antes de "project/" incluido el propio "project/"
            proyecto = proyectoUrl.substring(proyectoUrl.indexOf(prefijoProyecto) + prefijoProyecto.length());
        } else {
            // Si el usuario por error solo puso el nombre, lo usamos tal cual
            proyecto = proyectoUrl;
        }

        proyecto = proyecto.split("/")[0];

        // Obtener taiga_username de los miembros
        List<String> miembrosTaigaUsernames = usuarioRepository.findAllById(miembrosIds)
                .stream()
                .map(Usuario::getTaigaUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Usuario profesor = usuarioRepository.findById(profesorId)
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado."));
        String profesorTaigaUsername = profesor.getTaigaUsername();

        Map<String, Boolean> resultadoValidacion = new HashMap<>();
        resultadoValidacion.put("todosUsuariosTaigaConfigurados", miembrosTaigaUsernames.size() == miembrosIds.size());
        resultadoValidacion.put("profesorTaigaConfigurado", profesorTaigaUsername != null);

        try {
            // Verificar si el proyecto es privado. Si no es: professor y los alumnos son miembros del proyecto
           Map<String, Boolean> resultadosTaiga = verificarMiembros(proyecto, profesorTaigaUsername,miembrosTaigaUsernames);
           resultadoValidacion.put("proyectoPublico", resultadosTaiga.getOrDefault("proyectoPublico", false));
           resultadoValidacion.put("professoratEsMiembroT", resultadosTaiga.getOrDefault("professoratEsMiembroT", false));
            resultadoValidacion.put("todosMiembrosEnProyecto", resultadosTaiga.getOrDefault("todosMiembrosEnProyecto", false));

            // Si el proyecto es privado , devolver todos los valores como false
            if (!resultadoValidacion.get("proyectoPublico")) {
                resultadoValidacion.put("todosMiembrosEnProyecto", false);
                resultadoValidacion.put("professoratEsMiembroT", false);
                return resultadoValidacion;
            }

            // Si el professor no es miembro del proyecto
            if(!resultadoValidacion.get("professoratEsMiembroT")){
                resultadoValidacion.put("todosMiembrosEnProyecto", false);
                return resultadoValidacion;
            }

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error al comunicarse con la API de Taiga: " + e.getResponseBodyAsString());
        }

        return resultadoValidacion;
    }

    //2 Verificar si el proyecto es privado. Si no es: professor y los alumnos son miembros del proyecto
    private Map<String, Boolean> verificarMiembros(String proyecto, String profesorUsername, List<String> alumnosUsernames ) {
        Map<String, Boolean> resultados = new HashMap<>();
        // Valores por defecto para evitar NullPointerException
        resultados.put("proyectoPublico", false);
        resultados.put("professoratEsMiembroT", false);
        resultados.put("todosMiembrosEnProyecto", false);
        try {
            // Unimos la variable base con el endpoint específico del proyecto
            String url = String.format("%sprojects/by_slug?slug=%s", taigaApiBaseUrl, proyecto);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), JsonNode.class);
            JsonNode proyectoJson = response.getBody();
            if (proyectoJson != null) {
                // verifcar si proyecto es privado
                boolean isPrivate = proyectoJson.path("is_private").asBoolean();
                resultados.put("proyectoPublico", !isPrivate);

                if(isPrivate){
                    System.out.println("¡Error! el proyecto es PRIVADO.");
                }else{
                    //extraer lista de miembros
                    List<String> miembrosEnTaiga = proyectoJson.path("members").findValuesAsText("username");

                    //Validar profesor y alumnos en proyecto
                    resultados.put("professoratEsMiembroT", miembrosEnTaiga.contains(profesorUsername));
                    resultados.put("todosMiembrosEnProyecto", miembrosEnTaiga.containsAll(alumnosUsernames));
                }

            }

        } catch (Exception e) {
            System.err.println("Error al conectar con la API de Taiga: " + e.getMessage());
        }
        return resultados;
    }


    // 3. modificar bd si prj está bien
    public void asignarProyectp(Integer equipoId, String proyectoUrl) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalStateException("Equipo no encontrado"));

        String prefijoProyecto = "project/";

        String proyecto;
        if (proyectoUrl.contains(prefijoProyecto)) {
            // Cortamos todo lo que haya antes de "project/" incluido el propio "project/"
            proyecto = proyectoUrl.substring(proyectoUrl.indexOf(prefijoProyecto) + prefijoProyecto.length());
        } else {
            // Si el usuario por error solo puso el nombre, lo usamos tal cual
            proyecto = proyectoUrl;
        }
        String proyectoID;
        proyecto = proyecto.split("/")[0];

        Integer taigaProjectId = obtenerProyectoIdPorSlug(proyecto);

        if (taigaProjectId == null) {
            throw new IllegalStateException("No se pudo obtener el ID del proyecto en Taiga para el identificador: " + proyecto);
        }

        equipo.setTaigaProyectoId(taigaProjectId);
        equipo.setTaigaProyecto(proyecto);
        equipo = equipoRepository.save(equipo);

        cargarHistorias(equipo, equipo.getTaigaProyectoId());
        cargarTareasIniciales(equipo, equipo.getTaigaProyectoId());
    }

    //4. obtener id de proyecto Taiga a traves del nombre
    public Integer obtenerProyectoIdPorSlug(String proyecto) {
        String url = String.format("%sprojects/by_slug?slug=%s", taigaApiBaseUrl, proyecto);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().path("id").asInt();
            }
        } catch (Exception e) {
            System.err.println("Error al obtener ID del proyecto de Taiga: " + e.getMessage());
        }
        return null;
    }

    //5. desconectar proyecto
    public boolean desconectarProyecto(Integer equipoId) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(equipoId);
        if (equipoOpt.isPresent()) {
            Equipo equipo = equipoOpt.get();
            equipo.setTaigaProyecto(null);
            equipoRepository.save(equipo);
            return true;
        }
        return false;
    }

    //6. Obtener ID del Taiga user
    public Integer obtenerIdUser(String username) {
        String url = String.format("%susers?username=%s", taigaApiBaseUrl, username);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-disable-pagination", "true");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode usersArray = response.getBody();
                if (usersArray.isArray()) {
                    for (JsonNode userNode : usersArray) {
                        if (userNode.path("username").asText().equals(username)) {
                            return userNode.path("id").asInt();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al conectar con Taiga: " + e.getMessage());
        }
        return null;
    }

    //7. Caragamos por primera vez las historias del equipo
    public void cargarHistorias(Equipo equipo, Integer taigaProjectId) {


        // Llamamos a la API de historias de Taiga usando ese ID
        String url = String.format("%suserstories?project=%s", taigaApiBaseUrl, taigaProjectId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-disable-pagination", "true");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode historyArray = response.getBody();

                if (historyArray.isArray()) {
                    List<HistoriasUsuarioEquipo> historiasAGuardar = new ArrayList<>();

                    for (JsonNode storyNode : historyArray) {
                        JsonNode milestoneNode = storyNode.path("milestone_name");
                        String nombreSprint = (milestoneNode.isMissingNode() || milestoneNode.isNull())
                                ? null
                                : milestoneNode.asText();

                        HistoriasUsuarioEquipo historia = HistoriasUsuarioEquipo.builder()
                                .id(storyNode.path("id").asInt())
                                .titulo(storyNode.path("subject").asText())
                                .estado(storyNode.path("status_extra_info").path("name").asText("New"))
                                .sprint(nombreSprint)
                                .puntosEsfuerzo(storyNode.path("total_points").asInt(0))
                                .equipo(equipo)
                                .build();

                        historiasAGuardar.add(historia);
                    }

                    // Guardamos todas las historias
                    historiasRepository.saveAll(historiasAGuardar);
                    System.out.println("Carga inicial completada: " + historiasAGuardar.size() + " historias guardadas.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al descargar las historias de Taiga: " + e.getMessage());
        }
    }

    //8. Caragamos por primera vez las tareas del equipo
    public void cargarTareasIniciales(Equipo equipo, Integer taigaProjectId) {
        // Llamamos a la API de tareas de Taiga usando ese ID
        String url = String.format("%stasks?project=%s", taigaApiBaseUrl, taigaProjectId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-disable-pagination", "true");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode tasksArray = response.getBody();

                if (tasksArray.isArray()) {
                    List<TareasEquipo> tareasAGuardar = new ArrayList<>();

                    for (JsonNode taskNode : tasksArray) {

                        Estudiante estudianteAsignado = null;
                        JsonNode assignedToNode = taskNode.path("assigned_to");

                        // Comprobamos que el campo no sea nulo (la tarea podría estar sin asignar)
                        if (!assignedToNode.isMissingNode() && !assignedToNode.isNull()) {
                            Integer taigaIdAsignado = assignedToNode.asInt();

                            // Buscamos en nuestra BD al alumno que tiene ese ID de Taiga
                            estudianteAsignado = (Estudiante) usuarioRepository.findByTaigaId(taigaIdAsignado)
                                    .orElse(null); // Si no lo encuentra, lo dejamos a null
                        }
                        String fechaCreacionStr = taskNode.path("created_date").asText();
                        LocalDateTime fechaRealCreacion = java.time.OffsetDateTime.parse(fechaCreacionStr).toLocalDateTime();

                        LocalDateTime fechaRealCierre = null;
                        JsonNode finishedDateNode = taskNode.path("finished_date");
                        if (!finishedDateNode.isMissingNode() && !finishedDateNode.isNull()) {
                            String fechaCierreStr = finishedDateNode.asText();

                            if (!fechaCierreStr.isEmpty()) {
                                fechaRealCierre = java.time.OffsetDateTime.parse(fechaCierreStr).toLocalDateTime();
                            }
                        }

                        HistoriasUsuarioEquipo historiaVinculada = null;
                        JsonNode userStoryNode = taskNode.path("user_story");
                        if (!userStoryNode.isMissingNode() && !userStoryNode.isNull()) {
                            Integer idHistoriaTaiga = userStoryNode.asInt();
                            historiaVinculada = historiasRepository.findById(idHistoriaTaiga).orElse(null);
                        }
                        // ---------------------------------------------

                        TareasEquipo tarea = TareasEquipo.builder()
                                .id(taskNode.path("id").asInt())
                                .titulo(taskNode.path("subject").asText())
                                .estado(taskNode.path("status_extra_info").path("name").asText("New"))
                                .equipo(equipo)
                                .estudiante(estudianteAsignado)
                                .fechaCreacion(fechaRealCreacion)
                                .fechaCierre(fechaRealCierre)
                                .historiaUsuario(historiaVinculada)
                                .build();

                        tareasAGuardar.add(tarea);
                    }

                    // Guardamos todas las tareas
                    tareasRepository.saveAll(tareasAGuardar);
                    System.out.println("Carga inicial completada: " + tareasAGuardar.size() + " tareas guardadas.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al descargar las tareas de Taiga: " + e.getMessage());
        }
    }

    public HistoriasEquipoDTO calcularEstadisticasHistorias(Integer equipoId) {

        // 1. Obtener el equipo y sus estudiantes
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("El equipo con ID " + equipoId + " no existe."));

        List<Estudiante> todosLosEstudiantes = equipo.getEstudiantes();

        // 2. Obtener todas las historias y tareas del equipo
        List<HistoriasUsuarioEquipo> historiasDelEquipo = historiasRepository.findByEquipo_Id(equipoId);
        int totalHistoriasEquipo = historiasDelEquipo.size();

        int totalPuntosEquipo = historiasDelEquipo.stream()
                .mapToInt(HistoriasUsuarioEquipo::getPuntosEsfuerzo)
                .sum();

        List<TareasEquipo> tareasDelEquipo = tareasRepository.findByEquipoId(equipoId);

        // 3. Preparar la lista para el frontend
        List<HistoriasEstudianteDTO> metricasEstudiantes = new ArrayList<>();

        // 4. Recorrer a todos los estudiantes
        for (Estudiante estudiante : todosLosEstudiantes) {

            long historiasParticipadas = tareasDelEquipo.stream()
                    // a) Nos quedamos solo con las tareas de este estudiante
                    .filter(tarea -> tarea.getEstudiante() != null &&
                            tarea.getEstudiante().getId() == estudiante.getId())
                    // b) Ignoramos las tareas huérfanas (que no tienen historia asignada)
                    .filter(tarea -> tarea.getHistoriaUsuario() != null)
                    // d) Extraemos únicamente el ID de la historia de esas tareas
                    .map(tarea -> tarea.getHistoriaUsuario().getId())
                    // e) Filtramos para que no haya IDs repetidos
                    .distinct()
                    // f) Contamos cuántas historias únicas quedaron
                    .count();

            long historiasCerradas = tareasDelEquipo.stream()
                    .filter(tarea -> tarea.getEstudiante() != null && tarea.getEstudiante().getId() == estudiante.getId())
                    .filter(tarea -> tarea.getHistoriaUsuario() != null)
                    .filter(tarea -> "Closed".equalsIgnoreCase(tarea.getHistoriaUsuario().getEstado())) // El filtro extra
                    .map(tarea -> tarea.getHistoriaUsuario().getId())
                    .distinct()
                    .count();

            // Calcular el porcentaje de participación
            double porcentaje = 0.0;
            if (totalHistoriasEquipo > 0) {
                porcentaje = (historiasParticipadas * 100.0) / totalHistoriasEquipo;
            }

            // Construimos el DTO del estudiante
            HistoriasEstudianteDTO dto = new HistoriasEstudianteDTO();
            dto.setEstudianteId(estudiante.getId());
            dto.setNombreEstudiante(estudiante.getNombre());
            dto.setTotalHistoriasParticipadas((int) historiasParticipadas);
            dto.setPorcentajeHistorias(porcentaje);
            dto.setTotalHistoriasCerradas((int) historiasCerradas);
            metricasEstudiantes.add(dto);
        }

        // 5. Montar y devolver el DTO principal del Dashboard
        HistoriasEquipoDTO resultadoFinal = new HistoriasEquipoDTO();
        resultadoFinal.setTotalHistorias(totalHistoriasEquipo);
        resultadoFinal.setTotalPuntosEsfuerzoEquipo(totalPuntosEquipo);
        resultadoFinal.setMetricasEstudiantes(metricasEstudiantes);

        return resultadoFinal;
    }
    public TareasEquipoDTO calcularEstadisticasEquipoTareas(Integer equipoId, String proyecto) {

        // 1. Obtener el equipo y todos sus estudiantes matriculados
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("El equipo con ID " + equipoId + " no existe."));

        List<Estudiante> todosLosEstudiantes = equipo.getEstudiantes();

        // 2. Obtener todas las tareas de este equipo en la BD local
        List<TareasEquipo> tareasDelEquipo = tareasRepository.findByEquipoId(equipoId);
        int totalTareasEquipo = tareasDelEquipo.size();

        // 3. Preparar la list que viajará al frontend
        List<TareasEstudianteDTO> metricasEstudiantes = new ArrayList<>();

        // 4. Recorrer a TODOS los estudiantes del equipo (tengan tareas o no)
        for (Estudiante estudiante : todosLosEstudiantes) {

            // Filtramos la lista de tareas total para contar solo las que tienen asignado a este estudiante
            long tareasDelAlumno = tareasDelEquipo.stream()
                    .filter(tarea -> tarea.getEstudiante() != null &&
                            tarea.getEstudiante().getId() == estudiante.getId()) // <-- El cambio está aquí
                    .count();

            // Calculamos el porcentaje con cuidado de no dividir por cero si el equipo aún no tiene tareas
            double porcentaje = 0.0;
            if (totalTareasEquipo > 0) {
                porcentaje = (tareasDelAlumno * 100.0) / totalTareasEquipo;
            }

            // Construimos el DTO para la fila/columna de este estudiante en React
            TareasEstudianteDTO dto = new TareasEstudianteDTO();
            dto.setEstudianteId(estudiante.getId());
            dto.setNombreEstudiante(estudiante.getNombre());
            dto.setTotalTareas((int) tareasDelAlumno);
            dto.setPorcentajeTareas(porcentaje);

            metricasEstudiantes.add(dto);
        }

        // 5. Montar y devolver la "caja grande" (El DTO principal)
        TareasEquipoDTO resultadoFinal = new TareasEquipoDTO();
        resultadoFinal.setTotalTareasEquipo(totalTareasEquipo);
        resultadoFinal.setMetricasEstudiantes(metricasEstudiantes);

        return resultadoFinal;
    }

    public List<HistoriaDetalleDTO> obtenerDetallestaiga(Integer equipoId) {

        // 1. Buscamos el equipo en la base de datos
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado con ID: " + equipoId));

        // 2. Traemos todas las historias y tareas que pertenecen a este equipo
        List<HistoriasUsuarioEquipo> historiasDelEquipo = historiasRepository.findByEquipo_Id(equipoId);
        List<TareasEquipo> tareasDelEquipo = tareasRepository.findByEquipoId(equipoId);

        // 3. A partir de aquí, la lógica matemática es exactamente la misma que hicimos antes:
        List<HistoriaDetalleDTO> matrizDetalles = new ArrayList<>();
        List<Estudiante> estudiantesDelEquipo = equipo.getEstudiantes();

        for (HistoriasUsuarioEquipo historia : historiasDelEquipo) {
            HistoriaDetalleDTO dto = new HistoriaDetalleDTO();
            dto.setId(historia.getId());
            dto.setTitulo(historia.getTitulo());
            dto.setEstado(historia.getEstado());
            dto.setSprint(historia.getSprint());
            dto.setPuntosEsfuerzo(historia.getPuntosEsfuerzo());

            // Filtrar tareas de esta historia
            List<TareasEquipo> tareasDeEstaHistoria = tareasDelEquipo.stream()
                    .filter(tarea -> tarea.getHistoriaUsuario() != null && tarea.getHistoriaUsuario().getId().equals(historia.getId()))
                    .collect(Collectors.toList());

            dto.setTotalTareas(tareasDeEstaHistoria.size());

            long sinAsignar = tareasDeEstaHistoria.stream()
                    .filter(tarea -> tarea.getEstudiante() == null)
                    .count();
            dto.setTareasSinAsignar((int) sinAsignar);

            long miembrosUnicos = tareasDeEstaHistoria.stream()
                    .filter(tarea -> tarea.getEstudiante() != null)
                    .map(tarea -> tarea.getEstudiante().getId())
                    .distinct()
                    .count();
            dto.setTotalMiembros((int) miembrosUnicos);

            Map<String, Integer> recuentoPorEstudiante = new HashMap<>();
            for (Estudiante estudiante : estudiantesDelEquipo) {
                recuentoPorEstudiante.put(estudiante.getNombre(), 0);
            }

            for (TareasEquipo tarea : tareasDeEstaHistoria) {
                if (tarea.getEstudiante() != null) {
                    String nombreEstudiante = tarea.getEstudiante().getNombre();
                    int tareasActuales = recuentoPorEstudiante.getOrDefault(nombreEstudiante, 0);
                    recuentoPorEstudiante.put(nombreEstudiante, tareasActuales + 1);
                }
            }

            dto.setTareasPorEstudiante(recuentoPorEstudiante);
            matrizDetalles.add(dto);
        }

        return matrizDetalles;
    }

    private void guardarFechaUltimaSincronizacionTareas(Equipo equipo, LocalDateTime fecha) {
        // 1. Actualizamos el campo en el objeto equipo
        equipo.setUltimaSincronizacionTareas(fecha);

        // 2. Lo guardamos en la base de datos
        equipoRepository.save(equipo);

        System.out.println("Fecha de sincronización de tareas actualizada para el equipo: " + equipo.getNombre());
    }

    private void guardarFechaUltimaSincronizacionHistorias(Equipo equipo, LocalDateTime fecha) {
        // 1. Actualizamos el campo en el objeto equipo
        equipo.setUltimaSincronizacionHistorias(fecha);

        // 2. Lo guardamos en la base de datos
        equipoRepository.save(equipo);

        System.out.println("Fecha de sincronización de tareas actualizada para el equipo: " + equipo.getNombre());
    }

    public void sincronizarTareas(Integer equipoId) {

        // 1. Validar que el equipo existe
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        // 2. Obtener el ID numérico del proyecto en Taiga.
        Integer taigaProjectId = equipo.getTaigaProyectoId();

        LocalDateTime ultimaSincronizacion = equipo.getUltimaSincronizacionTareas();

        // 4. Construir la URL base
        String url = String.format("%stasks?project=%s", taigaApiBaseUrl, taigaProjectId);

        // 5. Si hay una fecha de última sincronización, la añadimos como filtro a la URL
        if (ultimaSincronizacion != null) {
            // Taiga espera formato ISO 8601 UTC (ej. 2026-02-12T10:30:00Z)
            String fechaFormateada = ultimaSincronizacion.atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
            url += "&modified_date__gte=" + fechaFormateada;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-disable-pagination", "true");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode tasksArray = response.getBody();
                if (tasksArray.isArray()) {


                    for (JsonNode taskNode : tasksArray) {

                        // Extraer fecha de creación
                        String fechaCreacionStr = taskNode.path("created_date").asText();
                        LocalDateTime fechaRealCreacion = java.time.OffsetDateTime.parse(fechaCreacionStr).toLocalDateTime();

                        // Extraer fecha de cierre con control de nulos
                        LocalDateTime fechaRealCierre = null;
                        JsonNode finishedDateNode = taskNode.path("finished_date");
                        if (!finishedDateNode.isMissingNode() && !finishedDateNode.isNull()) {
                            String fechaCierreStr = finishedDateNode.asText();
                            if (!fechaCierreStr.isEmpty()) {
                                fechaRealCierre = java.time.OffsetDateTime.parse(fechaCierreStr).toLocalDateTime();
                            }
                        }

                        // Buscar al estudiante asignado
                        Estudiante estudianteAsignado = null;
                        JsonNode assignedToNode = taskNode.path("assigned_to");
                        if (!assignedToNode.isMissingNode() && !assignedToNode.isNull()) {
                            Integer taigaIdAsignado = assignedToNode.asInt();
                            estudianteAsignado = (Estudiante) usuarioRepository.findByTaigaId(taigaIdAsignado)
                                    .orElse(null); // Si no lo encuentra, lo dejamos a null
                        }

                        HistoriasUsuarioEquipo historiaVinculada = null;
                        JsonNode userStoryNode = taskNode.path("user_story");
                        if (!userStoryNode.isMissingNode() && !userStoryNode.isNull()) {
                            Integer idHistoriaTaiga = userStoryNode.asInt();
                            historiaVinculada = historiasRepository.findById(idHistoriaTaiga).orElse(null);
                        }

                        TareasEquipo tarea = TareasEquipo.builder()
                                .id(taskNode.path("id").asInt())
                                .titulo(taskNode.path("subject").asText())
                                .estado(taskNode.path("status_extra_info").path("name").asText("New"))
                                .equipo(equipo)
                                .estudiante(estudianteAsignado)
                                .fechaCreacion(fechaRealCreacion)
                                .fechaCierre(fechaRealCierre)
                                .historiaUsuario(historiaVinculada)
                                .build();

                        tareasRepository.save(tarea);
                    }
                    guardarFechaUltimaSincronizacionTareas(equipo, LocalDateTime.now(ZoneOffset.UTC));
                }
            } else {
                throw new RuntimeException("La API de Taiga no devolvió un estado válido.");
            }
        }catch (Exception e) {
            System.err.println("Error al descargar las tareas de Taiga: " + e.getMessage());
        }
    }

    public void sincronizarHistorias(Integer equipoId) {

        // 1. Validar que el equipo existe
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        // 2. Obtener el ID numérico del proyecto en Taiga.
        Integer taigaProjectId =equipo.getTaigaProyectoId();


        LocalDateTime ultimaSincronizacion = equipo.getUltimaSincronizacionHistorias();

        // 4. Construir la URL base
        String url = String.format("%suserstories?project=%s", taigaApiBaseUrl, taigaProjectId);

        // 5. Añadir el filtro de fecha si no es la primera vez
        if (ultimaSincronizacion != null) {
            String fechaFormateada = ultimaSincronizacion.atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
            url += "&modified_date__gte=" + fechaFormateada;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("x-disable-pagination", "true");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode historyArray = response.getBody();

                if (historyArray.isArray()) {
                    List<HistoriasUsuarioEquipo> historiasAGuardar = new ArrayList<>();


                    for (JsonNode storyNode : historyArray) {
                        JsonNode milestoneNode = storyNode.path("milestone_name");
                        String nombreSprint = (milestoneNode.isMissingNode() || milestoneNode.isNull())
                                ? null
                                : milestoneNode.asText();
                        HistoriasUsuarioEquipo historia = HistoriasUsuarioEquipo.builder()
                                .id(storyNode.path("id").asInt())
                                .titulo(storyNode.path("subject").asText())
                                .estado(storyNode.path("status_extra_info").path("name").asText("New"))
                                .sprint(nombreSprint)
                                .puntosEsfuerzo(storyNode.path("total_points").asInt(0))
                                .equipo(equipo)
                                .build();

                        historiasAGuardar.add(historia);
                    }

                    // Guardamos todas las historias
                    historiasRepository.saveAll(historiasAGuardar);
                    System.out.println("Carga inicial completada: " + historiasAGuardar.size() + " historias guardadas.");
                }
                guardarFechaUltimaSincronizacionHistorias(equipo, LocalDateTime.now(ZoneOffset.UTC));
            }
        } catch (Exception e) {
            System.err.println("Error al descargar las historias de Taiga: " + e.getMessage());
        }
    }




}
