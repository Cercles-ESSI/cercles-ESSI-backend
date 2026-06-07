package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import tfg.backend_tfg.dto.*;
import tfg.backend_tfg.model.Evaluacion;
import tfg.backend_tfg.services.EvaluacionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    @Autowired
    private EvaluacionService evaluacionService;

    @PreAuthorize("hasAuthority('PROFESOR')")
    @PostMapping("/crear")
    public ResponseEntity<Evaluacion> crearEvaluacion(@RequestBody CrearEvaluacionDTO crearEvaluacionDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(evaluacionService.crearEvaluacion(crearEvaluacionDTO));
    }

    @PreAuthorize("hasAuthority('PROFESOR')")
    @GetMapping("/equipo/{equipoId}")
    public ResponseEntity<List<EvaluacionResumenDTO>> obtenerEvaluacionesPorEquipo(
            @PathVariable Integer equipoId,
            @RequestParam(required = false) List<Integer> evaluacionIds) {

        System.out.println("oki " + evaluacionIds);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<EvaluacionResumenDTO> evaluaciones = evaluacionService.obtenerEvaluacionesPorEquipo(equipoId, evaluacionIds);
        return ResponseEntity.ok(evaluaciones);
    }


    @PreAuthorize("hasAuthority('PROFESOR')")
    @GetMapping("/equipo/{equipoId}/medias")
    public ResponseEntity<List<EvaluacionMediaDTO>> obtenerMediasPorEquipo(@PathVariable Integer equipoId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<EvaluacionMediaDTO> medias = evaluacionService.obtenerMediasPorEquipo(equipoId);
        return ResponseEntity.ok(medias);
    }



    @GetMapping("/equipo/{equipoId}/evaluacion-activa") //Cambiar swagger
    public ResponseEntity<Map<String, Object>> getEvaluacionActiva(@PathVariable Integer equipoId) {
        Map<String, Object> response = evaluacionService.getEvaluacionActiva(equipoId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ESTUDIANTE')")
    @GetMapping("/equipo/{equipoId}/autoevaluacion/{estudianteId}")
    public ResponseEntity<List<MiAutoEvaluacionDTO>> obtenerMiAutoEvaluacion(
            @PathVariable Integer equipoId,
            @PathVariable Integer estudianteId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        List<MiAutoEvaluacionDTO> misNotas = evaluacionService.obtenerMisAutoEvaluaciones(equipoId, estudianteId);

        return ResponseEntity.ok(misNotas);
    }

    @GetMapping("/equipo/{equipoId}/evaluacion-activa-id")
    public ResponseEntity<Integer> obtenerEvaluacionActivaId(@PathVariable Integer equipoId) {
        Integer evaluacionId = evaluacionService.obtenerEvaluacionActiva(equipoId);
        if (evaluacionId != null) {
            return ResponseEntity.ok(evaluacionId);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    @GetMapping("/equipo/evaluacion-realizada/{evaluacionId}/{estudianteId}")
    public ResponseEntity<Boolean> isEvaluacionRealizada(@PathVariable Integer estudianteId, @PathVariable Integer evaluacionId) {
        boolean realizada = evaluacionService.existsByEvaluacionIdAndEvaluadorId(evaluacionId, estudianteId);
        return ResponseEntity.ok(realizada);
    }

    @PreAuthorize("hasAuthority('ESTUDIANTE')")
    @PostMapping("/estudiante/crear")
    public ResponseEntity<String> crearEvaluacionDetalle(@RequestBody EvaluacionDetalleRequestDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        evaluacionService.crearEvaluacionDetalle(request.getEvaluacionId(), request.getEvaluadorId(), request.getDetalles());
        return ResponseEntity.ok("Evaluación guardada correctamente");
    }

    @GetMapping("/count/{cursoId}")
    public ResponseEntity<?> contarEvaluacionesPorCurso(@PathVariable Integer cursoId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
            List<Integer> evaluacionesIds = evaluacionService.obtenerIdsEvaluacionesPorCurso(cursoId);
            return ResponseEntity.ok(Map.of("idsEvaluaciones", evaluacionesIds));
        
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al contar las evaluaciones"));
        }
    }


}
