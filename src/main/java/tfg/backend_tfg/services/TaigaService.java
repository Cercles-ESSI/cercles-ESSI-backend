package tfg.backend_tfg.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

        proyecto = proyecto.split("/")[0];
        equipo.setTaigaProyecto(proyecto);
        equipoRepository.save(equipo);
    }

    //4. desconectar proyecto
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


}
