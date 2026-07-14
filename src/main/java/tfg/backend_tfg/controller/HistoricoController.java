package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tfg.backend_tfg.services.HistoricoService;
import tfg.backend_tfg.dto.HistoricoFrontendDTO;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
public class HistoricoController {
    @Autowired
    private HistoricoService historicoService;

    // Protegemos el endpoint para que solo el profesor pueda acceder
    @PreAuthorize("hasAuthority('PROFESOR')")
    @GetMapping("/equipo/{equipoId}/historico")
    public ResponseEntity<List<HistoricoFrontendDTO>> obtenerHistoricoEquipo(
            @PathVariable Integer equipoId) {

        // Llamamos al servicio que buscará en la tabla datos_historico y agrupará los datos
        List<HistoricoFrontendDTO> datosHistoricos = historicoService.obtenerDatosDashboard(equipoId);

        return ResponseEntity.ok(datosHistoricos);
    }
}
