package tfg.backend_tfg.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import tfg.backend_tfg.dto.EquipoDetalleDTO;
import tfg.backend_tfg.dto.EquipoSummaryDTO;
import tfg.backend_tfg.dto.EstudianteDTO;
import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.Estudiante;
import tfg.backend_tfg.model.EstudianteCursoId;
import tfg.backend_tfg.model.EstudianteEquipo;
import tfg.backend_tfg.model.EstudianteEquipoId;
import tfg.backend_tfg.model.Profesor;
import tfg.backend_tfg.model.ProfesorCurso;
import tfg.backend_tfg.model.ProfesorCursoId;
import tfg.backend_tfg.repository.CursoRepository;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.EstudianteCursoRepository;
import tfg.backend_tfg.repository.EstudianteEquipoRepository;
import tfg.backend_tfg.repository.EstudianteRepository;
import tfg.backend_tfg.repository.ProfesorCursoRepository;
import tfg.backend_tfg.repository.ProfesorRepository;
import tfg.backend_tfg.repository.UsuarioRepository;
import tfg.backend_tfg.security.TokenEncrypter;

@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private EstudianteEquipoRepository estudianteEquipoRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfesorCursoRepository profesorCursoRepository;

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private EstudianteCursoRepository estudianteCursoRepository;

    private final TokenEncrypter tokenEncrypter;

    @Autowired
    public EquipoService(TokenEncrypter tokenEncrypter) {
        this.tokenEncrypter = tokenEncrypter;
    }

    public String getTokenEquipo(int idEquipo){
        return equipoRepository.findByEquipoId(idEquipo).getTokenGithubAsignatura();
    }



    public void validarEstudianteCurso(int estudianteId, int cursoId) {
        // Crear la clave compuesta
        EstudianteCursoId id = new EstudianteCursoId(estudianteId, cursoId);
        //buscar en caso de que sea profesor
        ProfesorCursoId id2 = new ProfesorCursoId(estudianteId, cursoId);
    
        // Verificar si existe la relación entre estudiante y curso
        boolean perteneceAlCurso = estudianteCursoRepository.existsById(id) || profesorCursoRepository.existsById(id2);
    
        if (!perteneceAlCurso) {
            throw new IllegalArgumentException("El estudiante no pertenece al curso especificado.");
        }
    }

    public List<EquipoSummaryDTO> obtenerEquiposPorCurso(int cursoId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new IllegalArgumentException("El curso especificado no existe.");
        }

        return equipoRepository.findByCursoId(cursoId)
                .stream()
                .map(equipo -> new EquipoSummaryDTO(
                        equipo.getId(), 
                        equipo.getNombre(), 
                        equipo.getCurso().getId(), 
                        equipo.getEvaluador().getId(),
                        equipo.getCurso().getNombreAsignatura(),
                        equipo.getCurso().isActivo()))
                .collect(Collectors.toList());
    }

    public List<EquipoSummaryDTO> obtenerEquiposPorUsuario(int usuarioId) {
        // Validar que el usuario existe
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new IllegalArgumentException("El usuario especificado no existe.");
        }
    
        // Obtener los equipos donde el usuario es estudiante
        List<Equipo> equiposComoEstudiante = estudianteEquipoRepository.findEquiposByEstudianteId(usuarioId);
    
        // Obtener los equipos donde el usuario es evaluador
        List<Equipo> equiposComoEvaluador = equipoRepository.findByEvaluadorId(usuarioId);
    
        // Combinar ambas listas y eliminar duplicados (si existieran)
        List<Equipo> equiposUnicos = Stream.concat(equiposComoEstudiante.stream(), equiposComoEvaluador.stream())
                .distinct()
                .collect(Collectors.toList());
    
        // Mapear a DTO
        return equiposUnicos.stream()
                .map(equipo -> new EquipoSummaryDTO(
                    equipo.getId(), 
                    equipo.getNombre(), 
                    equipo.getCurso().getId(), 
                    equipo.getEvaluador().getId(),
                    equipo.getCurso().getNombreAsignatura(),
                    equipo.getCurso().isActivo()))
                .collect(Collectors.toList());
    }
    

    public EquipoDetalleDTO obtenerEquipoDetallePorId(int id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El equipo especificado no existe."));

        // Obtener detalles del curso
        Curso curso = equipo.getCurso();

        // Obtener detalles del evaluador
        int evaluadorId = equipo.getEvaluador().getId();
        String evaluadorNombre = equipo.getEvaluador().getNombre();
        String evaluadorCorreo = equipo.getEvaluador().getCorreo();
        String taigaUserProf = equipo.getEvaluador().getTaigaUsername();

        String gitOrganizacion = equipo.getGitOrganizacion();
        String taigaProyecto = equipo.getTaigaProyecto();

        // Obtener estudiantes del equipo usando el repositorio
        List<EstudianteDTO> estudiantes = estudianteEquipoRepository.findByEquipoId(id)
        .stream()
        .map(estudianteEquipo -> {
            Estudiante estudiante = estudianteEquipo.getEstudiante();
            return new EstudianteDTO(
                    estudiante.getId(),
                    estudiante.getNombre(),
                    estudiante.getCorreo(),
                    null
            );
        })
        .collect(Collectors.toList());

        String tokenDescifrado = null;
        try {
            if (curso.getTokenGithubAsignatura() != null) {
                tokenDescifrado = tokenEncrypter.decrypt(curso.getTokenGithubAsignatura());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el token del curso.", e);
        }

        // Crear y devolver el DTO
        return new EquipoDetalleDTO(
                equipo.getId(),
                equipo.getNombre(),
                curso.getId(),
                curso.getNombreAsignatura(),
                curso.getAñoInicio(),
                curso.getCuatrimestre(),
                curso.isActivo(),
                curso.getGithubAsignatura(),
                tokenDescifrado,
                curso.getGestionTareas(),
                evaluadorId,
                evaluadorNombre,
                evaluadorCorreo,
                taigaUserProf,
                estudiantes,
                gitOrganizacion,
                taigaProyecto
        );
    }

    // Método para encontrar equipo por ID
    public Equipo obtenerEquipoPorId(Integer equipoId) {
        return equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado con ID: " + equipoId));
    }

    // Método para actualizar un equipo
    public void actualizarEquipo(Equipo equipo) {
        equipoRepository.save(equipo);
    }
    
    

    public Equipo crearEquipo(String nombre, int cursoId, int evaluadorId, List<Integer> estudiantesIds) {
        // Validar que el curso existe
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("El curso especificado no existe."));
    
        // Validar que el nombre del equipo sea único en el curso
        if (equipoRepository.existsByNombreAndCursoId(nombre, cursoId)) {
            throw new IllegalArgumentException("Ya existe un equipo con este nombre en el curso.");
        }
    
        // Validar que el profesor existe y pertenece al curso
        Profesor profesorEvaluador = profesorRepository.findById(evaluadorId)
                .orElseThrow(() -> new IllegalArgumentException("El profesor especificado no existe."));
        boolean profesorEnCurso = profesorCursoRepository.existsByProfesorIdAndCursoId(evaluadorId, cursoId);
        if (!profesorEnCurso) {
            throw new IllegalArgumentException("El profesor no pertenece al curso.");
        }
    
        // Crear el equipo
        Equipo equipo = new Equipo();
        equipo.setNombre(nombre);
        equipo.setCurso(curso);
        equipo.setEvaluador(profesorEvaluador);
        equipo = equipoRepository.save(equipo);
    
        // Validar y asociar los estudiantes al equipo
        List<Estudiante> estudiantes = estudianteRepository.findAllById(estudiantesIds);
        if (estudiantes.size() != estudiantesIds.size()) {
            throw new IllegalArgumentException("Uno o más estudiantes especificados no existen.");
        }
    
        for (Estudiante estudiante : estudiantes) {
            boolean estudianteEnCurso = estudianteCursoRepository.existsByEstudianteIdAndCursoId(estudiante.getId(), cursoId);
            if (!estudianteEnCurso) {
                throw new IllegalArgumentException("El estudiante " + estudiante.getId() + " no pertenece al curso.");
            }
    
            EstudianteEquipo estudianteEquipo = new EstudianteEquipo(estudiante, equipo);
            estudianteEquipoRepository.save(estudianteEquipo);
        }
    
        return equipo;
    }
    
    public void borrarEquipo(int equipoId) {
        if (!equipoRepository.existsById(equipoId)) {
            throw new IllegalArgumentException("El equipo con el ID proporcionado no existe.");
        }

        // Borrar el equipo
        equipoRepository.deleteById(equipoId);
    }

    public void salirseDelEquipo(int equipoId, int estudianteId) {
        // Verificar si el equipo existe
        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("El equipo con el ID proporcionado no existe."));

        // Verificar si el estudiante está en el equipo
        EstudianteEquipo estudianteEquipo = estudianteEquipoRepository.findByEstudianteIdAndEquipoId(estudianteId, equipoId)
                .orElseThrow(() -> new IllegalArgumentException("El estudiante no pertenece a este equipo."));

        // Borrar la relación entre el estudiante y el equipo
        estudianteEquipoRepository.delete(estudianteEquipo);

        // Verificar si el equipo se quedó sin miembros
        long miembrosRestantes = estudianteEquipoRepository.countByEquipoId(equipoId);
        if (miembrosRestantes == 0) {
            equipoRepository.delete(equipo);
        }
    }

    public void añadirMiembros(int equipoId, List<Integer> estudiantesIds) {
        // Validar que el equipo existe
        Equipo equipo = equipoRepository.findById(equipoId)
            .orElseThrow(() -> new IllegalArgumentException("El equipo no existe."));
    
        for (int estudianteId : estudiantesIds) {
            // Validar que el estudiante existe
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("El estudiante con ID " + estudianteId + " no existe."));
    
            // Verificar que el estudiante no esté ya en el equipo
            boolean yaEsMiembro = estudianteEquipoRepository.existsByEstudianteIdAndEquipoId(estudianteId, equipoId);
    
            if (yaEsMiembro) {
                throw new IllegalArgumentException("El estudiante con ID " + estudianteId + " ya es miembro del equipo.");
            }
    
            // Crear la relación entre el estudiante y el equipo
            EstudianteEquipo nuevoMiembro = new EstudianteEquipo(estudiante, equipo);
            estudianteEquipoRepository.save(nuevoMiembro);
        }
    }

    public void borrarMiembros(int equipoId, List<Integer> estudiantesIds) {
        // Validar que el equipo existe
        Equipo equipo = equipoRepository.findById(equipoId)
            .orElseThrow(() -> new IllegalArgumentException("El equipo no existe."));
    
        for (int estudianteId : estudiantesIds) {
            // Validar que el estudiante existe
            Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("El estudiante con ID " + estudianteId + " no existe."));
    
            // Verificar que el estudiante sea miembro del equipo
            EstudianteEquipo relacion = estudianteEquipoRepository.findByEstudianteIdAndEquipoId(estudianteId, equipoId)
                .orElseThrow(() -> new IllegalArgumentException("El estudiante con ID " + estudianteId + " no pertenece al equipo."));
    
            // Eliminar la relación entre el estudiante y el equipo
            estudianteEquipoRepository.delete(relacion);
        }
    }
    
    
}
