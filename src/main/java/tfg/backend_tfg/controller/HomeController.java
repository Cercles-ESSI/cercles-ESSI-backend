package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tfg.backend_tfg.model.Rol;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.services.HomeService;
import tfg.backend_tfg.dto.DashboardHomeDTO;
import tfg.backend_tfg.services.UsuarioService;

import java.util.Optional;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    @Autowired
    private HomeService homeService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/resumen-inicio")
    public ResponseEntity<?> obtenerResumenCurso() {


        // Obtener la información de autenticación desde el SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(403).body("Usuario no autenticado.");
        }

        // Obtener el correo del usuario autenticado
        String email = authentication.getName();

        // Buscar el usuario en la base de datos usando el correo electrónico
        Optional<Usuario> usuarioOpt = usuarioService.getOptUsuarioByCorreo(email);

        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();
        Rol rolUsuario = usuario.getRol();
        Integer usuarioId = usuario.getId();
        DashboardHomeDTO dashboardData = homeService.obtenerResumenInicial(usuarioId, email, rolUsuario);
        return ResponseEntity.ok(dashboardData);
    }

}