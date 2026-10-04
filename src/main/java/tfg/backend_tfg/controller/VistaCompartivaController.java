package tfg.backend_tfg.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tfg.backend_tfg.dto.ComparativaDetalleDTO;
import tfg.backend_tfg.services.VistaComparativaService;

import java.util.Map;

@RestController
@RequestMapping("/api/comparativa")
public class VistaCompartivaController {

    private final VistaComparativaService comparativaService;

    public VistaCompartivaController(VistaComparativaService comparativaService) {
        this.comparativaService = comparativaService;
    }

    @PreAuthorize("hasAuthority('profesor')")
    @GetMapping("/comparativa-equips/{cursoId}")
    public ResponseEntity<?> obtenerComparativa(@PathVariable Integer cursoId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(403).body(Map.of("error", "Usuario no autenticado"));
            }
            ComparativaDetalleDTO comparativas = comparativaService.getMetricasEquipoCompleto(cursoId);

            return ResponseEntity.ok(comparativas);

        } catch (Exception e) {
            System.err.println("Fallo en la obtencion de datos: " + e.getMessage());
            return ResponseEntity.badRequest().body("Fallo en la obtencion de datos: " + e.getMessage());
        }



    }
}
