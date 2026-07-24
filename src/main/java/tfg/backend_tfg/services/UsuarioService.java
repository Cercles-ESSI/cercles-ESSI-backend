package tfg.backend_tfg.services;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import tfg.backend_tfg.model.Estudiante;
import tfg.backend_tfg.model.EstudianteCurso;
import tfg.backend_tfg.model.EstudianteEquipo;
import tfg.backend_tfg.model.Profesor;
import tfg.backend_tfg.model.Rol;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.repository.EstudianteCursoRepository;
import tfg.backend_tfg.repository.EstudianteEquipoRepository;
import tfg.backend_tfg.repository.EstudianteRepository;
import tfg.backend_tfg.repository.ProfesorCursoRepository;
import tfg.backend_tfg.repository.UsuarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private final UsuarioRepository usuarioRepository;
    @Autowired
    private final EstudianteRepository estudianteRepository;
    @Autowired
    private final ProfesorCursoRepository profesorCursoRepository;
    @Autowired
    private final EstudianteCursoRepository estudianteCursoRepository;
    @Autowired
    private final EstudianteEquipoRepository estudianteEquipoRepository;

    @Autowired
    private final TaigaService taigaService;
    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, EstudianteRepository estudianteRepository, ProfesorCursoRepository profesorCursoRepository, EstudianteCursoRepository estudianteCursoRepository, EstudianteEquipoRepository estudianteEquipoRepository, TaigaService taigaService) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.profesorCursoRepository = profesorCursoRepository;
        this.estudianteCursoRepository = estudianteCursoRepository;
        this.estudianteEquipoRepository = estudianteEquipoRepository;
        this.taigaService = taigaService;

    }



    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    public boolean esUsuarioParteDelCurso(int usuarioId, int cursoId) {
        boolean esEstudiante = estudianteCursoRepository.findByEstudianteIdAndCursoId(usuarioId, cursoId).isPresent();
        boolean esProfesor = profesorCursoRepository.findByProfesorIdAndCursoId(usuarioId, cursoId).isPresent();
        return esEstudiante || esProfesor;
    }
    

    public List<String> getAllUsuariosById(List<Integer> estudiantesIds) {
        return estudianteRepository.findAllById(estudiantesIds)
                    .stream()
                    .map(Estudiante::getGitUsername)
                    .filter(username -> username != null && !username.isEmpty())
                    .collect(Collectors.toList());

    }

    public Optional<Usuario> getUsuarioById(int id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> getOptUsuarioByCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public List<Usuario> getAllUsuariosByRol(Rol rol) {
        return usuarioRepository.findAllByRol(rol);
    }

    public int getUsuarioByCorreo(String correoAutenticado) {
        return usuarioRepository.findByCorreo(correoAutenticado)
        .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado."))
        .getId();
    }

    public List<EstudianteCurso> getCursosEstudiante (int id) {
        return estudianteCursoRepository.findByEstudianteId(id);
    } 

    public boolean getEquiposEstudianteYaPresente(EstudianteCurso ec, int id) {
        return estudianteEquipoRepository.findByEstudianteIdAndCursoId(id, ec.getCurso().getId()).isEmpty();
    }

    public ResponseEntity<?> crearUsuario(String nombre, String correo, Rol rol) {
        try {
            // Verificar si ya existe un usuario con el correo dado
            Optional<Usuario> usuarioExistente = usuarioRepository.findByCorreo(correo);

            if (usuarioExistente.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Usuario con el correo ya existe");
            }

            Usuario usuario;
            String taigaUsername = correo.substring(0, correo.indexOf("@"));

            if (rol == Rol.Estudiante) {
                usuario = Estudiante.builder()
                        .correo(correo)
                        .nombre(nombre)
                        .taigaUsername(taigaUsername)
                        .rol(rol)
                        .build();
            } else if (rol == Rol.Profesor) {
                usuario = Profesor.builder()
                        .correo(correo)
                        .nombre(nombre)
                        .taigaUsername(taigaUsername)
                        .rol(rol)
                        .build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rol inválido");
            }

            // Guardar el nuevo usuario en la base de datos
            usuarioRepository.save(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el usuario: " + e.getMessage());
        }
    }

}
