package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.security.TokenEncrypter;
import tfg.backend_tfg.services.EquipoService;
import tfg.backend_tfg.services.GithubService;
import tfg.backend_tfg.services.UsuarioService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GithubService githubService;
    @Autowired
    private final UsuarioService usuarioService;
    @Autowired
    private final EquipoService equipoService;
    private final TokenEncrypter tokenEncrypter;
    @Autowired
    private StringHttpMessageConverter stringHttpMessageConverter;


    public GitHubController(GithubService githubService, UsuarioService usuarioService, EquipoService equipoService, TokenEncrypter tokenEncrypter) {
        this.githubService = githubService;
        this.usuarioService = usuarioService;
        this.equipoService = equipoService;
        this.tokenEncrypter = tokenEncrypter;
    }

    @PostMapping("/validar-organizacion")
    public ResponseEntity<?> validarOrganizacion(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            String organizacionUrl = (String) request.get("organizacionUrl");
            List<Integer> miembrosIds = (List<Integer>) request.get("miembrosIds");
            Integer profesorId = Integer.valueOf(request.get("profesorId").toString());
            String githubAsignatura = (String) request.get("githubAsignatura");
            String tokenGithub = (String) request.get("tokenGithub");

            Map<String, Boolean> resultado = githubService.validarOrganizacion(profesorId, miembrosIds, organizacionUrl, githubAsignatura, tokenGithub);

            return ResponseEntity.ok(resultado);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Error al comunicarse con la API de GitHub: " + e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }


    @PostMapping("/confirmar-organizacion")
    public ResponseEntity<?> confirmarOrganizacion(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            Integer equipoId = (Integer) request.get("equipoId");
            String organizacionUrl = (String) request.get("organizacionUrl");

            githubService.asignarOrganizacion(equipoId, organizacionUrl);

            return ResponseEntity.ok(Map.of("mensaje", "Organización asignada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @DeleteMapping("/disconnect-organizacion")
    public ResponseEntity<?> desconectarOrganizacion(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            Integer equipoId = (Integer) request.get("equipoId");

            boolean desconectado = githubService.desconectarOrganizacion(equipoId);

            if (desconectado) {
                return ResponseEntity.ok(Map.of("mensaje", "Organización desconectada correctamente."));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Equipo no encontrado."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('PROFESOR')")
    @GetMapping("/equipo/{idEquipo}/metrics/{organizacion}")
    public ResponseEntity<?> obtenerMetricas(@PathVariable String organizacion, @RequestParam List<Integer> estudiantesIds, @PathVariable Integer idEquipo, @RequestParam Boolean isGithub) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            // Obtener usuarios GitHub de los estudiantes
            List<String> usuarios = usuarioService.getAllUsuariosById(estudiantesIds);

            String tokenGithub = equipoService.getTokenEquipo(idEquipo);

            String tokenDescifrado = null;
            try {
                if (tokenGithub != null) {
                    tokenDescifrado = tokenEncrypter.decrypt(tokenGithub);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error al descifrar el token del curso.", e);
            }
            // Llamada al servicio con el token personal
            Map<String, Object> metricasOrganizacion = githubService.obtenerMetricasOrganizacion(
                    organizacion, usuarios, tokenDescifrado, estudiantesIds, isGithub
            );

            return ResponseEntity.ok(metricasOrganizacion);
        } catch (Exception e) {
            e.printStackTrace(); // Log completo del error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    
    @PostMapping("/callback")
    public ResponseEntity<?> handleGitHubCallback(@RequestBody Map<String, String> requestBody) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            String email = authentication.getName();
            String code = requestBody.get("code");

            if (code == null || code.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Código de GitHub faltante"));
            }

            Map<String, String> result = githubService.handleGitHubCallback(email, code);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    @GetMapping("/user-data")
    public ResponseEntity<?> obtenerDatosGitHub() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            String email = authentication.getName();
            Map<String, Object> datosUsuario = githubService.obtenerDatosUsuarioGitHub(email);

            if (datosUsuario == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Datos no encontrados para este usuario."));
            }

            return ResponseEntity.ok(datosUsuario);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<?> desconectarGitHub() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            String email = authentication.getName();
            githubService.desconectarGitHub(email);
            return ResponseEntity.ok(Map.of("message", "GitHub desconectado exitosamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
    }

}
