package tfg.backend_tfg.services;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tfg.backend_tfg.dto.IssueGithubDTO;
import tfg.backend_tfg.dto.MetricasLineasUsuarioDTO;
import tfg.backend_tfg.dto.MetricasUsuarioDTO;
import tfg.backend_tfg.model.*;
import tfg.backend_tfg.repository.*;
import tfg.backend_tfg.security.TokenEncrypter;

@Service
public class GithubService {

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private MetricasEstudianteRepository metricasEstudianteRepository;
    @Autowired
    private TareasEquipoRepository tareasRepository;

    @Autowired
    private HistoriasUsuarioEquipoRepository historiasRepository;

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private TokenEncrypter tokenEncrypter;

    //1-8 funciones datos de una org

    //1. validar la org de un equipo
    public Map<String, Boolean> validarOrganizacion(Integer profesorId, List<Integer> miembrosIds, String organizacionUrl, String githubAsignatura, String tokenGithub) {
        String organizacion = organizacionUrl.replace("https://github.com/", "").replaceAll("/$", "");

        // Obtener git_username de los miembros
        List<String> miembrosGitUsernames = usuarioRepository.findAllById(miembrosIds)
                .stream()
                .map(Usuario::getGitUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Usuario profesor = usuarioRepository.findById(profesorId)
                .orElseThrow(() -> new RuntimeException("Profesor no encontrado."));
        String profesorGitUsername = profesor.getGitUsername();

        Map<String, Boolean> resultadoValidacion = new HashMap<>();
        resultadoValidacion.put("todosUsuariosGitConfigurados", miembrosGitUsernames.size() == miembrosIds.size());
        resultadoValidacion.put("profesorGitConfigurado", profesorGitUsername != null);

        try {
            // Verificar si el github de la asignatura es miembro
            boolean professoratMiembro = verificarMiembro(organizacion, githubAsignatura, tokenGithub);
            resultadoValidacion.put("professoratEsMiembro", professoratMiembro);

            // Si el github de la asignatura no es miembro, devolver todos los valores como false
            if (!professoratMiembro) {
                resultadoValidacion.put("professoratEsAdmin", false);
                resultadoValidacion.put("todosMiembrosEnOrganizacion", false);
                resultadoValidacion.put("profesorEnOrganizacion", false);
                return resultadoValidacion;
            }

            // Verificar si el github de la asignatura es admin
            boolean professoratOwner = verificarOwner(organizacion, githubAsignatura, tokenGithub);
            resultadoValidacion.put("professoratEsAdmin", professoratOwner);

            if (!professoratOwner) {
                resultadoValidacion.put("todosMiembrosEnOrganizacion", false);
                resultadoValidacion.put("profesorEnOrganizacion", false);
                return resultadoValidacion;
            }

            // Obtener miembros de la organización
            String url = String.format("https://api.github.com/orgs/%s/members", organizacion);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenGithub);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            List<String> miembrosOrg = response.getBody().findValuesAsText("login");

            // Validar miembros y profesor
            boolean todosMiembrosValidos = miembrosOrg.containsAll(miembrosGitUsernames);
            boolean profesorValido = miembrosOrg.contains(profesorGitUsername);

            resultadoValidacion.put("todosMiembrosEnOrganizacion", todosMiembrosValidos);
            resultadoValidacion.put("profesorEnOrganizacion", profesorValido);

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error al comunicarse con la API de GitHub: " + e.getResponseBodyAsString());
        }

        return resultadoValidacion;
    }

    //2 verificar si el github de la asignatura es miembro
    private boolean verificarMiembro(String organizacion, String username, String tokenGithub) {
        try {
            String url = String.format("https://api.github.com/orgs/%s/members/%s", organizacion, username);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenGithub);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<?> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            return true; // Si no lanza excepción, es miembro
        } catch (HttpClientErrorException.NotFound e) {
            return false; // No es miembro
        }
    }

    //3 verificar que el github de la asignatura es admin/owner
    private boolean verificarOwner(String organizacion, String username, String tokenGithub) {
        try {
            String url = String.format("https://api.github.com/orgs/%s/memberships/%s", organizacion, username);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + tokenGithub);
            headers.set("Accept", "application/vnd.github+json");

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            return "admin".equals(response.getBody().path("role").asText());
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error al verificar owner: " + e.getResponseBodyAsString());
        }
    }

    // 4. modificar bd si org está bien
    public void asignarOrganizacion(Integer equipoId, String organizacionUrl) {
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalStateException("Equipo no encontrado"));

        equipo.setGitOrganizacion(organizacionUrl.replace("https://github.com/", "").replaceAll("/$", ""));
        equipoRepository.save(equipo);
        List<String> gitUsernames = equipo.getEstudiantes().stream()
                .map(Estudiante::getGitUsername) // Extrae el username
                .filter(username -> username != null && !username.isEmpty()) // Filtra si alguno no tiene usuario
                .collect(Collectors.toList());

        List<Integer> estudiantesIds = equipo.getEstudiantes().stream()
                .map(Estudiante::getId)
                .collect(Collectors.toList());

        Boolean gestionProyecto = false;
        String tokenGithub = equipoService.getTokenEquipo(equipoId);

        String tokenDescifrado = null;
        try {
            if (tokenGithub != null) {
                tokenDescifrado = tokenEncrypter.decrypt(tokenGithub);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el token del curso.", e);
        }

        if (tokenDescifrado != null) {
            final String tokenParaHilo = tokenDescifrado;
            CompletableFuture.runAsync(() -> {
                obtenerMetricasOrganizacion(
                        equipo.getGitOrganizacion(),
                        gitUsernames,
                        tokenParaHilo,
                        estudiantesIds,
                        gestionProyecto,
                        equipoId
                );
            });
        }
    }

    //5. desconectar organizacion
    public boolean desconectarOrganizacion(Integer equipoId) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(equipoId);
        if (equipoOpt.isPresent()) {
            Equipo equipo = equipoOpt.get();
            equipo.setGitOrganizacion(null);
            equipo.setUltimaSincronizacionGit(null);
            metricasEstudianteRepository.deleteByEquipoId(equipoId);
            equipoRepository.save(equipo);
            return true;
        }
        return false;
    }
    

    // 5. Obtener repositorios de la organización
    public List<String> obtenerRepositorios(String organizacion, String accessToken) {
        String url = "https://api.github.com/orgs/" + organizacion + "/repos";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        List<String> repositorios = new ArrayList<>();
        response.getBody().forEach(repo -> repositorios.add(repo.get("name").asText()));
        return repositorios;
    }

    //7. comprobar si algun repo está vacio
    public boolean isRepositorioVacio(String organizacion, String repo, String accessToken) {
        try {
            //si no ha habido ningun commit entendemos que está vacío
            String commitsUrl = "https://api.github.com/repos/" + organizacion + "/" + repo + "/commits?per_page=1";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github+json");
            HttpEntity<?> entity = new HttpEntity<>(headers);
    
            ResponseEntity<JsonNode> response = restTemplate.exchange(commitsUrl, HttpMethod.GET, entity, JsonNode.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("Error al verificar commits del repositorio: " + response.getStatusCode());
                return true; // Asumimos vacío si hay un error.
            }
    
            JsonNode body = response.getBody();
            return body == null || body.isEmpty(); // Si no hay commits, el repositorio está vacío.
        } catch (Exception e) {
            System.err.println("Error al verificar si el repositorio está vacío: " + e.getMessage());
            return true; // Asumimos vacío en caso de excepción.
        }
    }
    
    

    //6. obtener metricas de un repo
    public Map<String, Object> obtenerMetricasRepositorio(String organizacion, String repo, List<String> usuarios, String accessToken, Boolean gestionProyecto, String sinceDateIso ){
        String commitsBaseUrl = "https://api.github.com/repos/" + organizacion + "/" + repo + "/commits";
        String pullsBaseUrl = "https://api.github.com/repos/" + organizacion + "/" + repo + "/pulls";
    
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<?> entity = new HttpEntity<>(headers);
    
        Map<String, MetricasUsuarioDTO> metricsMap = new HashMap<>();
        for (String usuario : usuarios) {
            metricsMap.put(usuario, new MetricasUsuarioDTO(usuario));
        }
    
        List<String> globalIssueDetails = new ArrayList<>();

        procesarCommitsYPRsGraphQL(organizacion, repo, accessToken, metricsMap, sinceDateIso);

        if(gestionProyecto){
            try{
                // Obtener Issues y Métricas Globales
                Map<String, Object> repoGlobalMetrics = obtenerMetricasGlobalesIssues(organizacion, repo, metricsMap, accessToken, sinceDateIso);

                // Agregar detalles de issues al listado global
                globalIssueDetails.addAll((List<String>) repoGlobalMetrics.get("issueDetails"));

            } catch (Exception e) {
                System.err.println("Error al obtener métricas del repositorio " + repo + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("userMetrics", new ArrayList<>(metricsMap.values()));
        result.put("globalIssueDetails", globalIssueDetails);
    
        return result;
    }

    //obtener commits, lineaas y pull requests utilizando graphql
    private void procesarCommitsYPRsGraphQL(String organizacion, String repo, String accessToken, Map<String, MetricasUsuarioDTO> metricsMap, String sinceDateIso) {
        String graphqlUrl = "https://api.github.com/graphql";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        String query = """
        query($owner: String!, $repo: String!, $cursorCommits: String, $cursorPRs: String, $sinceDate: GitTimestamp) {
          repository(owner: $owner, name: $repo) {
            defaultBranchRef {
              target {
                ... on Commit {
                  history(first: 100, after: $cursorCommits, since: $sinceDate) {
                    pageInfo { hasNextPage endCursor }
                    edges {
                      node {
                        message additions deletions
                        author { user { login } }
                      }
                    }
                  }
                }
              }
            }
            pullRequests(first: 100, after: $cursorPRs) {
              pageInfo { hasNextPage endCursor }
              nodes { author { login } createdAt }
            }
          }
        }
        """;

        boolean hasNextCommits = true;
        String cursorCommits = null;
        boolean hasNextPRs = true;
        String cursorPRs = null;

        try {
            while (hasNextCommits || hasNextPRs) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("owner", organizacion);
                variables.put("repo", repo);
                variables.put("cursorCommits", cursorCommits);
                variables.put("cursorPRs", cursorPRs);
                variables.put("sinceDate", sinceDateIso);

                Map<String, Object> payload = Map.of("query", query, "variables", variables);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(graphqlUrl, entity, JsonNode.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    System.err.println("Error al obtener datos de GraphQL: " + response.getStatusCode());
                    break;
                }

                JsonNode repositoryNode = response.getBody().path("data").path("repository");

                // 1. Procesar Commits
                if (hasNextCommits) {
                    JsonNode history = repositoryNode.path("defaultBranchRef").path("target").path("history");
                    if (!history.isMissingNode()) {
                        for (JsonNode edge : history.path("edges")) {
                            JsonNode commitNode = edge.path("node");
                            String author = commitNode.path("author").path("user").path("login").asText(null);
                            String message = commitNode.path("message").asText("");
                            int additions = commitNode.path("additions").asInt(0);
                            int deletions = commitNode.path("deletions").asInt(0);

                            if (author != null && metricsMap.containsKey(author)) {
                                MetricasUsuarioDTO metrics = metricsMap.get(author);
                                if (message.startsWith("Merge pull request") || message.startsWith("Merge branch") || message.startsWith("Merge remote-tracking branch")) {
                                    metrics.setPullRequestsMerged(metrics.getPullRequestsMerged() + 1);
                                } else {
                                    metrics.setTotalCommits(metrics.getTotalCommits() + 1);
                                    metrics.setLinesAdded(metrics.getLinesAdded() + additions);
                                    metrics.setLinesRemoved(metrics.getLinesRemoved() + deletions);
                                }
                            }
                        }
                        hasNextCommits = history.path("pageInfo").path("hasNextPage").asBoolean(false);
                        cursorCommits = history.path("pageInfo").path("endCursor").asText(null);
                    } else {
                        hasNextCommits = false;
                    }
                }

                // 2. Procesar Pull Requests
                if (hasNextPRs) {
                    JsonNode pullRequests = repositoryNode.path("pullRequests");
                    if (!pullRequests.isMissingNode()) {
                        for (JsonNode prNode : pullRequests.path("nodes")) {

                            // Si hay fecha since, ignoramos las PRs más antiguas que esa fecha
                            if (sinceDateIso != null) {
                                String createdAtStr = prNode.path("createdAt").asText();
                                Instant prDate = Instant.parse(createdAtStr);
                                Instant sinceDate = Instant.parse(sinceDateIso);
                                if (prDate.isBefore(sinceDate)) {
                                    continue; // Saltamos esta PR porque es vieja
                                }
                            }

                            String author = prNode.path("author").path("login").asText(null);
                            if (author != null && metricsMap.containsKey(author)) {
                                MetricasUsuarioDTO metrics = metricsMap.get(author);
                                metrics.setPullRequestsCreated(metrics.getPullRequestsCreated() + 1);
                            }
                        }
                        hasNextPRs = pullRequests.path("pageInfo").path("hasNextPage").asBoolean(false);
                        cursorPRs = pullRequests.path("pageInfo").path("endCursor").asText(null);
                    } else {
                        hasNextPRs = false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Excepción procesando GraphQL para el repositorio " + repo + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    

    //7. get issues globales (gestion de proyecto)
    public Map<String, Object> obtenerMetricasGlobalesIssues(String organizacion, String repo, Map<String, MetricasUsuarioDTO> metricsMap, String accessToken, String sinceDateIso) {
        List<IssueGithubDTO> listaIssueDtos = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;

        // 1. Construimos el filtro de fecha si existe
        String sinceFilter = (sinceDateIso != null) ? ", filterBy: {since: \"" + sinceDateIso + "\"}" : "";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        // 2. Definimos la query de GraphQL
        String query = "query($owner: String!, $repo: String!, $cursor: String) { " +
                "  repository(owner: $owner, name: $repo) { " +
                "    issues(first: 100, after: $cursor" + sinceFilter + ") { " +
                "      pageInfo { hasNextPage endCursor } " +
                "      nodes { " +
                "        number title state createdAt closedAt " +
                "        author { login } " +
                "        assignees(first: 10) { nodes { login } } " +
                "        labels(first: 20) { nodes { name } } " +
                "      } " +
                "    } " +
                "  } " +
                "}";

        String graphqlUrl = "https://api.github.com/graphql";

        while (hasNextPage) {
            // 3. Preparamos el Body de la petición
            Map<String, Object> variables = new HashMap<>();
            variables.put("owner", organizacion);
            variables.put("repo", repo);
            variables.put("cursor", cursor);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("variables", variables);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                // 4. Hacemos la petición POST
                ResponseEntity<JsonNode> response = restTemplate.exchange(graphqlUrl, HttpMethod.POST, entity, JsonNode.class);
                JsonNode body = response.getBody();

                // Comprobamos si hay errores de GraphQL
                if (body != null && body.has("errors")) {
                    System.err.println("Error de GraphQL: " + body.get("errors").toString());
                    break;
                }

                // 5. Navegamos por el JSON de respuesta
                JsonNode issuesConnection = body.path("data").path("repository").path("issues");
                JsonNode issuesNodes = issuesConnection.path("nodes");

                for (JsonNode issue : issuesNodes) {
                    Integer number = issue.path("number").asInt();
                    String title = issue.path("title").asText();

                    String state = issue.path("state").asText();
                    boolean isClosed = state.equalsIgnoreCase("CLOSED");

                    LocalDateTime fechaCreacion = ZonedDateTime.parse(issue.path("createdAt").asText()).toLocalDateTime();
                    LocalDateTime fechaCierre = issue.path("closedAt").isNull() ? null
                            : ZonedDateTime.parse(issue.path("closedAt").asText()).toLocalDateTime();

                    String author = issue.path("author").path("login").asText();

                    List<String> assignees = new ArrayList<>();
                    issue.path("assignees").path("nodes").forEach(a -> assignees.add(a.path("login").asText()));

                    List<String> labels = new ArrayList<>();
                    issue.path("labels").path("nodes").forEach(label -> labels.add(label.path("name").asText().toLowerCase()));

                    boolean isUserStory = labels.contains("user story") || labels.contains("historia de usuario") || labels.contains("història d'usuari");
                    boolean isTask = labels.contains("task") || labels.contains("tarea") || labels.contains("tasca");


                    if (isUserStory || isTask) {
                        String labelType = isUserStory ? "USER_STORY" : "TASK";
                        IssueGithubDTO issueDto = new IssueGithubDTO();
                        issueDto.setNumber(number);
                        issueDto.setTitle(title);
                        issueDto.setState(state);
                        issueDto.setCreatedAt(fechaCreacion);
                        issueDto.setClosedAt(fechaCierre);
                        issueDto.setType(labelType);
                        issueDto.setAssignees(assignees);

                        listaIssueDtos.add(issueDto);

                        for (String assignee : assignees) {
                            if (metricsMap.containsKey(assignee)) {
                                MetricasUsuarioDTO metrics = metricsMap.get(assignee);
                                if (isUserStory) {
                                    metrics.setUserStories(metrics.getUserStories() + 1);
                                    if (isClosed) metrics.setUserStoriesClosed(metrics.getUserStoriesClosed() + 1);
                                }
                                if (isTask) {
                                    metrics.setTasks(metrics.getTasks() + 1);
                                    if (isClosed) metrics.setTasksClosed(metrics.getTasksClosed() + 1);
                                }
                            }
                        }
                    }
                }

                // 6. Actualizamos la paginación para el siguiente ciclo
                JsonNode pageInfo = issuesConnection.path("pageInfo");
                hasNextPage = pageInfo.path("hasNextPage").asBoolean();
                if (hasNextPage) {
                    cursor = pageInfo.path("endCursor").asText();
                }

            } catch (Exception e) {
                System.err.println("Error al obtener issues con GraphQL: " + e.getMessage());
                hasNextPage = false;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("issueDtos", listaIssueDtos);
        return result;
    }
    
    
    //8. obtener metricas de una org
    public void obtenerMetricasOrganizacion(String organizacion, List<String> usuarios, String accessToken, List<Integer> estudiantesIds, Boolean gestionProyecto, Integer equipoId) {
        List<String> repositorios = obtenerRepositorios(organizacion, accessToken);

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        //Fecha para indicar cual fue la ultima vez que se actualizo los datos
        String sinceDateIsoTemp = null;
        if (equipo.getUltimaSincronizacionGit() != null) {
            sinceDateIsoTemp = equipo.getUltimaSincronizacionGit()
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);
        }
        final String sinceDateIso = sinceDateIsoTemp;

        List<IssueGithubDTO> globalIssuesRecopilados = new ArrayList<>();
        List<Estudiante> estudiantesList = estudianteRepository.findAllById(estudiantesIds);
        Map<String, Estudiante> usernameToEstudiante = estudiantesList.stream()
                .collect(Collectors.toMap(
                        Estudiante::getGitUsername,
                        estudiante -> estudiante
                ));

        Map<String, MetricasUsuarioDTO> aggregatedMetrics = new HashMap<>();
        for (String usuario : usuarios) {
            Estudiante est = usernameToEstudiante.get(usuario);
            String nombre = (est != null) ? est.getNombre() : "Desconocido";
            aggregatedMetrics.put(usuario, new MetricasUsuarioDTO(nombre, usuario));
        }

        // 1. Crear un pool de hilos adaptado al número de repositorios
        int numberOfThreads = Math.min(repositorios.size(), 30);
        Executor customExecutor = Executors.newFixedThreadPool(Math.max(numberOfThreads, 1));

        // 2. Procesar todos los repositorios en paralelo usando el customExecutor
        List<CompletableFuture<Map<String, Object>>> futures = repositorios.stream()
                .map(repo -> CompletableFuture.supplyAsync(
                        () -> obtenerMetricasRepositorio(organizacion, repo, usuarios, accessToken, gestionProyecto, sinceDateIso),
                        customExecutor // Pasamos el pool de hilos optimizado
                ))
                .toList();

        // 3. Esperar a que se completen todas las tareas
        for (CompletableFuture<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> repoMetrics = future.join();
                if (repoMetrics != null && !repoMetrics.isEmpty()) {
                    List<MetricasUsuarioDTO> userMetrics = (List<MetricasUsuarioDTO>) repoMetrics.get("userMetrics");
                    List<IssueGithubDTO> repoIssues = (List<IssueGithubDTO>) repoMetrics.get("issueDtos");

                    for (MetricasUsuarioDTO userMetric : userMetrics) {
                        MetricasUsuarioDTO aggregatedMetric = aggregatedMetrics.get(userMetric.getUsername());
                        if (aggregatedMetric != null) {
                            aggregatedMetric.combine(userMetric);
                        }
                    }
                    if (repoIssues != null) {
                        globalIssuesRecopilados.addAll(repoIssues);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error procesando métricas de repositorio: " + e.getMessage());
            }
        }
        guardarMetricasBasicas(aggregatedMetrics, usernameToEstudiante, equipo);

        if (Boolean.TRUE.equals(gestionProyecto)) {
            guardarIssuesDeProyecto(globalIssuesRecopilados, usernameToEstudiante, equipo);
        }

        equipo.setUltimaSincronizacionGit(LocalDateTime.now());
        equipoRepository.save(equipo);

    }

    //Funcion para guardar métricas de código (Commits, PRs, etc.)
    private void guardarMetricasBasicas(Map<String, MetricasUsuarioDTO> aggregatedMetrics, Map<String, Estudiante> usernameToEstudiante, Equipo equipo) {
        List<MetricasEstudiante> metricasParaGuardar = new ArrayList<>();

        for (MetricasUsuarioDTO dto : aggregatedMetrics.values()) {
            Estudiante estudiante = usernameToEstudiante.get(dto.getUsername());

            if (estudiante != null) {
                MetricasEstudiante metricas = metricasEstudianteRepository
                        .findByEstudianteIdAndEquipoId(estudiante.getId(), equipo.getId())
                        .orElse(new MetricasEstudiante());

                metricas.setEstudiante(estudiante);
                metricas.setEquipo(equipo);

                metricas.setTotalCommits( (metricas.getTotalCommits() == null ? 0 : metricas.getTotalCommits()) + dto.getTotalCommits() );
                metricas.setLinesAdded( (metricas.getLinesAdded() == null ? 0 : metricas.getLinesAdded()) + dto.getLinesAdded() );
                metricas.setLinesRemoved( (metricas.getLinesRemoved() == null ? 0 : metricas.getLinesRemoved()) + dto.getLinesRemoved() );
                metricas.setPullRequestsMerged( (metricas.getPullRequestsMerged() == null ? 0 : metricas.getPullRequestsMerged()) + dto.getPullRequestsMerged() );

                metricasParaGuardar.add(metricas);
            }
        }

        System.out.println("Carga inicial completada: " + metricasParaGuardar.size() + " metricas guardadas.");
        metricasEstudianteRepository.saveAll(metricasParaGuardar);
    }

    //Funcion para guardar las métricas relacionadas con las issues
    private void guardarIssuesDeProyecto(List<IssueGithubDTO> globalIssuesRecopilados, Map<String, Estudiante> usernameToEstudiante, Equipo equipo) {
        for (IssueGithubDTO dto : globalIssuesRecopilados) {

            Estudiante responsable = null;

            if (dto.getAssignees() != null && !dto.getAssignees().isEmpty()) {
                for (int i = dto.getAssignees().size() - 1; i >= 0; i--) {
                    String login = dto.getAssignees().get(i);
                    if (usernameToEstudiante.containsKey(login)) {
                        responsable = usernameToEstudiante.get(login);
                        break;
                    }
                }
            }

            String estadoTraducido = "CLOSED".equalsIgnoreCase(dto.getState()) ? "Cerrada" : "Abierta";

            if ("USER_STORY".equals(dto.getType())) {
                HistoriasUsuarioEquipo historia = historiasRepository
                        .findByIdHistoriaAndEquipoId(dto.getNumber(), equipo.getId())
                        .orElse(new HistoriasUsuarioEquipo());

                historia.setIdHistoria(dto.getNumber());
                historia.setTitulo(dto.getTitle());
                historia.setEstado(estadoTraducido);
                historia.setEquipo(equipo);
                historia.setResponsable(responsable);

                historiasRepository.save(historia);

            } else if ("TASK".equals(dto.getType())) {
                TareasEquipo tarea = tareasRepository
                        .findByIdTareaAndEquipoId(dto.getNumber(), equipo.getId())
                        .orElse(new TareasEquipo());

                tarea.setIdTarea(dto.getNumber());
                tarea.setTitulo(dto.getTitle());
                tarea.setEstado(estadoTraducido);
                tarea.setEquipo(equipo);
                tarea.setEstudiante(responsable);
                tarea.setFechaCreacion(dto.getCreatedAt());
                tarea.setFechaCierre(dto.getClosedAt());

                tareasRepository.save(tarea);
            }
        }
    }

    //Consultar la BD para obtener las metricas
    public Map<String, Object> consultarMetricasOrganizacion(Integer equipoId) {
        List<MetricasEstudiante> metricasBD = metricasEstudianteRepository.findByEquipoId(equipoId);

        List<MetricasUsuarioDTO> userMetrics = new ArrayList<>();

        for (MetricasEstudiante metrica : metricasBD) {
            Estudiante estudiante = metrica.getEstudiante();

            MetricasUsuarioDTO dto = new MetricasUsuarioDTO(estudiante.getNombre(), estudiante.getGitUsername());

            dto.setTotalCommits(metrica.getTotalCommits() != null ? metrica.getTotalCommits() : 0);
            dto.setLinesAdded(metrica.getLinesAdded() != null ? metrica.getLinesAdded() : 0);
            dto.setLinesRemoved(metrica.getLinesRemoved() != null ? metrica.getLinesRemoved() : 0);
            dto.setPullRequestsMerged(metrica.getPullRequestsMerged() != null ? metrica.getPullRequestsMerged() : 0);

            userMetrics.add(dto);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userMetrics", userMetrics);
        result.put("globalIssueDetails", new ArrayList<String>());

        return result;
    }




    //11-15 funciones datos usuario

    // 11. obtener el nombre de usuario de github
    public Pair<String, String> obtenerNombreUsuarioGitHub(String code) {
        String accessTokenUrl = "https://github.com/login/oauth/access_token";
        String userApiUrl = "https://api.github.com/user";

        System.out.println("Client ID: " + clientId);
        System.out.println("Client Secret: " + clientSecret);
        System.out.println("Authorization Code: " + code);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // Paso 1: Intercambiar el código por un access token
            HttpPost tokenRequest = new HttpPost(accessTokenUrl);
            tokenRequest.setHeader("Accept", "application/json");
            tokenRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));
            params.add(new BasicNameValuePair("code", code));
            tokenRequest.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));

            try (CloseableHttpResponse response = client.execute(tokenRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("Respuesta del token: " + responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode tokenJson = objectMapper.readTree(responseBody);

                JsonNode accessTokenNode = tokenJson.get("access_token");
                if (accessTokenNode == null || accessTokenNode.asText().isEmpty()) {
                    System.err.println("No se encontró el access_token en la respuesta");
                    return null;
                }

                String accessToken = accessTokenNode.asText();

                // Paso 2: Usar el access token para obtener el nombre de usuario de GitHub
                HttpGet userRequest = new HttpGet(userApiUrl);
                userRequest.setHeader("Authorization", "Bearer " + accessToken);
                userRequest.setHeader("Accept", "application/json");

                try (CloseableHttpResponse userResponse = client.execute(userRequest)) {
                    String userResponseBody = EntityUtils.toString(userResponse.getEntity());
                    System.out.println("Respuesta del usuario: " + userResponseBody);

                    JsonNode userJson = objectMapper.readTree(userResponseBody);
                    JsonNode loginNode = userJson.get("login");

                    if (loginNode == null || loginNode.asText().isEmpty()) {
                        System.err.println("No se encontró el nombre de usuario de GitHub en la respuesta");
                        return null;
                    }

                    String username = loginNode.asText();
                    return Pair.of(username, accessToken);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //12. conectar usuario con su github
    public Map<String, String> handleGitHubCallback(String email, String code) throws Exception {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(email);
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario usuario = usuarioOpt.get();
        Pair<String, String> githubData = obtenerNombreUsuarioGitHub(code);

        if (githubData == null) {
            throw new IllegalStateException("Error al obtener datos de GitHub");
        }

        usuario.setGitUsername(githubData.getFirst());
        usuario.setGithubAccessToken(githubData.getSecond());
        usuarioRepository.save(usuario);

        return Map.of("message", "Cuenta de GitHub asociada exitosamente", "githubUsername", githubData.getFirst());
    }

    //13. buscar datos de github de un usuario
    public Map<String, Object> obtenerDatosUsuarioGitHub(String email) throws Exception {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(email);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getGithubAccessToken() == null) {
            return null;
        }

        String accessToken = usuarioOpt.get().getGithubAccessToken();

        String reposUrl = "https://api.github.com/user/repos";
        String orgsUrl = "https://api.github.com/user/orgs";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        List<Map<String, Object>> repos = fetchGitHubData(reposUrl, entity);
        List<Map<String, Object>> orgs = fetchGitHubData(orgsUrl, entity);

        return Map.of(
            "repositorios", repos,
            "organizaciones", orgs
        );
    }

    //14. fetch datos de github
    private List<Map<String, Object>> fetchGitHubData(String url, HttpEntity<?> entity) {
        try {
            ResponseEntity<List> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, List.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    //15. desconectar github de usuario 
    public void desconectarGitHub(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setGitUsername(null);
            usuario.setGithubAccessToken(null);
            usuarioRepository.save(usuario);
        }
    }

    
}
