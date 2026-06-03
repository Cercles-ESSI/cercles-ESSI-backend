package tfg.backend_tfg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import tfg.backend_tfg.dto.CursoSummaryDTO;
import tfg.backend_tfg.dto.EquipoSummaryDTO;
import tfg.backend_tfg.model.EstudianteCurso;
import tfg.backend_tfg.model.Rol;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.services.EquipoService;
import tfg.backend_tfg.services.UsuarioService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

     @Autowired
    private EquipoService equipoService;


    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> getAllUsuarios() {
        return usuarioService.getAllUsuarios();
    }

    @PreAuthorize("hasAuthority('profesor')")
    @PostMapping("/crear")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, Object> requestBody) {
        // Verificar autenticación del usuario
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autenticado.");
        }
        String nombre = (String) requestBody.get("nombre");
        String correo = (String) requestBody.get("correo");
        Rol rol = Rol.valueOf((String) requestBody.get("rol"));
        // Llamar al servicio para crear el usuario
        return usuarioService.crearUsuario(nombre, correo, rol);
    }

    @GetMapping("/profesores")
    public ResponseEntity<?> getAllProfesores() {
        try {
            // Verificar autenticación del usuario
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario no autenticado.");
            }

            // Obtener el listado de profesores
            List<Usuario> profesores = usuarioService.getAllUsuariosByRol(Rol.Profesor);
            List<Map<String, Object>> response = profesores.stream().map(profesor -> {
                Map<String, Object> profesorData = new HashMap<>();
                profesorData.put("id", profesor.getId());
                profesorData.put("nombre", profesor.getNombre());
                profesorData.put("correo", profesor.getCorreo());
                return profesorData;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener los profesores: " + e.getMessage());
        }
    }

    

    @GetMapping("/{id}")
    public Optional<Usuario> getUsuarioById(@PathVariable int id) {
        return usuarioService.getUsuarioById(id);
    }

    @GetMapping("/datos")
    public ResponseEntity<?> getDatosUsuario() {
        try {
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

            // Devolver los datos del usuario
            Usuario usuario = usuarioOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("nombre", usuario.getNombre());
            response.put("correo", usuario.getCorreo());
            response.put("gitUsername", usuario.getGitUsername());
            response.put("taigaUsername", usuario.getTaigaUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno del servidor: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ESTUDIANTE') or hasAuthority('PROFESOR')")
    @GetMapping("/{usuario_id}/equipos")
    public ResponseEntity<List<EquipoSummaryDTO>> obtenerEquiposDeUsuario(@PathVariable int usuario_id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        // Obtener el ID del usuario desde el token JWT
        String correoAutenticado = authentication.getName();
        int idAutenticado = usuarioService.getUsuarioByCorreo(correoAutenticado);

        // Verificar si el usuario autenticado tiene acceso a los datos solicitados
        if (idAutenticado != usuario_id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        try {
            // Llamar al servicio para obtener los equipos como DTOs
            List<EquipoSummaryDTO> equipos = equipoService.obtenerEquiposPorUsuario(usuario_id);
            return ResponseEntity.ok(equipos);
        } catch (IllegalArgumentException e) {
            // Manejar errores específicos
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            // Manejar errores genéricos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PreAuthorize("hasAuthority('ESTUDIANTE')")
    @GetMapping("/{id}/cursos")
    public ResponseEntity<List<CursoSummaryDTO>> obtenerCursosDisponibles(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        // Obtener el ID del usuario desde el token JWT
        String correoAutenticado = authentication.getName();
        int idAutenticado = usuarioService.getUsuarioByCorreo(correoAutenticado);

        // Verificar si el usuario autenticado tiene acceso a los datos solicitados
        if (idAutenticado != id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        try {
            // Obtener todos los cursos del estudiante
            List<EstudianteCurso> cursosEstudiante = usuarioService.getCursosEstudiante(id);

            // Filtrar cursos donde el estudiante ya tenga equipo
            List<CursoSummaryDTO> cursosSinEquipo = cursosEstudiante.stream()
            .filter(ec -> usuarioService.getEquiposEstudianteYaPresente(ec, id))
            .map(ec -> new CursoSummaryDTO(
                    ec.getCurso().getId(),
                    ec.getCurso().getNombreAsignatura(),
                    ec.getCurso().getAñoInicio(),
                    ec.getCurso().getCuatrimestre(),
                    ec.getCurso().isActivo(),
                    0,
                    0,
                    0,
                    ec.getCurso().getGestionTareas()
            ))
                    .toList();

            return ResponseEntity.ok(cursosSinEquipo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
