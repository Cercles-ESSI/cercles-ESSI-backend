package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.repository.UsuarioRepository;
import tfg.backend_tfg.services.TaigaService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/taiga")
public class TaigaController {

    private final TaigaService taigaService;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public TaigaController(TaigaService taigaService, UsuarioRepository usuarioRepository) {
        this.taigaService = taigaService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/validar-proyecto")
    public ResponseEntity<?> validarProyecto(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            String proyectoUrl = (String) request.get("proyectoUrl");
            List<Integer> miembrosIds = (List<Integer>) request.get("miembrosIds");
            Integer profesorId = Integer.valueOf(request.get("profesorId").toString());
            String profesorTaiga = (String) request.get("profesorTaiga");

            Map<String, Boolean> resultado = taigaService.validarProyecto(profesorId, miembrosIds, profesorTaiga, proyectoUrl);

            return ResponseEntity.ok(resultado);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error al comunicarse con la API de Taiga: " + e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @PostMapping("/confirmar-proyecto")
    public ResponseEntity<?> confirmarProyecto(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            Integer equipoId = (Integer) request.get("equipoId");
            String proyectoUrl = (String) request.get("proyectoUrl");

            taigaService.asignarProyectp(equipoId, proyectoUrl);

            return ResponseEntity.ok(Map.of("mensaje", "Proyecto asignado correctamente."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

    @DeleteMapping("/disconnect-proyecto")
    public ResponseEntity<?> desconectarProyecto(@RequestBody Map<String, Object> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }

            Integer equipoId = (Integer) request.get("equipoId");

            boolean desconectado = taigaService.desconectarProyecto(equipoId);

            if (desconectado) {
                return ResponseEntity.ok(Map.of("mensaje", "Proyecto desconectado correctamente."));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Equipo no encontrado."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor: " + e.getMessage()));
        }
    }

}
