package tfg.backend_tfg.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import tfg.backend_tfg.dto.*;
import tfg.backend_tfg.model.*;
import tfg.backend_tfg.repository.CursoRepository;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.EstudianteCursoRepository;
import tfg.backend_tfg.repository.EstudianteEquipoRepository;
import tfg.backend_tfg.repository.EvaluacionRepository;
import tfg.backend_tfg.repository.ProfesorCursoRepository;
import tfg.backend_tfg.repository.UsuarioRepository;
import tfg.backend_tfg.security.TokenEncrypter;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cursoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TaigaService taigaService;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private EstudianteCursoRepository estudianteCursoRepository;

    @Autowired
    private EstudianteEquipoRepository estudianteEquipoRepository;

    @Autowired
    private ProfesorCursoRepository profesorCursoRepository;

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    private final TokenEncrypter tokenEncrypter;
    @Autowired
    public CursoService(TokenEncrypter tokenEncrypter) {
        this.tokenEncrypter = tokenEncrypter;
    }

    public Optional<Curso> getCursoById(int id) {
        return cursoRepository.findById(id);
    }

    public void save(Curso cursoExistente) {
        cursoRepository.save(cursoExistente);
    }
    //Leer estudiantes del excel con validaciones
    public ResponseEntity<?> uploadEstudiantes(MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Por favor sube un archivo Excel válido.");
            }

            List<Map<String, String>> estudiantes = new ArrayList<>();
            Set<String> correosExistentes = new HashSet<>();
            List<String> errores = new ArrayList<>();
            
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    // Verificar si la fila está completamente vacía
                    boolean isRowEmpty = true;
                    for (int cellIndex = 0; cellIndex < 3; cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                            isRowEmpty = false;
                            break;
                        }
                    }
            
                    // Si la fila está completamente vacía, continuar con la siguiente
                    if (isRowEmpty) {
                        continue;
                    }
            
                    // Obtener los valores de las celdas
                    String grupo = row.getCell(0) != null ? row.getCell(0).getStringCellValue().trim() : null;
                    String cognomsNom = row.getCell(1) != null ? row.getCell(1).getStringCellValue().trim() : null;
                    String correo = row.getCell(2) != null ? row.getCell(2).getStringCellValue().trim() : null;
            
                    // Validar que no hay datos incompletos
                    if (grupo == null || cognomsNom == null || correo == null) {
                        errores.add("Fila " + (i + 1) + ": Manquen dades obligatòries (grup, nom o correu).");
                        continue;
                    }
            
                    // Validar el formato del correo electrónico
                    if (!correo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        errores.add("Fila " + (i + 1) + ": Correu electrònic amb format incorrecte.");
                        continue;
                    }
            
                    // Validar duplicados por correo electrónico
                    if (correosExistentes.contains(correo)) {
                        errores.add("Fila " + (i + 1) + ": Correu duplicat (" + correo + ").");
                        continue;
                    }
            
                    correosExistentes.add(correo);
            
                    // Separar apellidos y nombre
                    String[] partes = cognomsNom.split(",");
                    if (partes.length < 2) {
                        errores.add("Fila " + (i + 1) + ": Format incorrecte en el nom i cognoms.");
                        continue;
                    }
            
                    String apellidos = partes[0].trim();
                    String nombre = partes[1].trim();
                    String nombreCompleto = nombre + " " + apellidos;
            
                    // Agregar estudiante a la lista
                    Map<String, String> estudianteData = new HashMap<>();
                    estudianteData.put("nombre", nombreCompleto);
                    estudianteData.put("correo", correo);
                    estudianteData.put("grupo", grupo);
                    estudiantes.add(estudianteData);
                }
            }            
            workbook.close();

            // Si hay errores, devolverlos en la respuesta
            if (!errores.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "S'han trobat errors en l'arxiu.",
                    "errores", errores
                ));
            }

            return ResponseEntity.ok(estudiantes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar el archivo: " + e.getMessage());
        }
    }


    //Crear curso
    public ResponseEntity<?> crearCurso(CursoRequest cursoRequest, String profesorEmail) {
        try {
            // Verificar si ya existe un curso similar activo
            Optional<Curso> cursoExistente = cursoRepository.findByNombreAsignaturaAndAñoInicioAndCuatrimestreAndActivo(
                    cursoRequest.getNombreAsignatura(), cursoRequest.getAñoInicio(), cursoRequest.getCuatrimestre(), true);

            if (cursoExistente.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Curso activo ya existente. ¿Desea desactivarlo?");
            }

            String encryptedToken = null;
            if (cursoRequest.getTokenGithubAsignatura() != null && !cursoRequest.getTokenGithubAsignatura().isEmpty()) {
                encryptedToken = tokenEncrypter.encrypt(cursoRequest.getTokenGithubAsignatura());
            }

            System.out.println("git " + cursoRequest.getGithubAsignatura());
            System.out.println("token " + cursoRequest.getTokenGithubAsignatura());
            // Crear el curso
            Curso curso = Curso.builder()
                    .nombreAsignatura(cursoRequest.getNombreAsignatura())
                    .añoInicio(cursoRequest.getAñoInicio())
                    .cuatrimestre(cursoRequest.getCuatrimestre())
                    .activo(cursoRequest.isActivo())
                    .githubAsignatura(cursoRequest.getGithubAsignatura())
                    .tokenGithubAsignatura(encryptedToken)
                    .gestionTareas(cursoRequest.getGestionTareas())
                    .build();

            curso = cursoRepository.save(curso);

            // Asignar profesores al curso
            ResponseEntity<?> responseProfesores = añadirProfesores(curso, cursoRequest.getProfesores());
            if (!responseProfesores.getStatusCode().equals(HttpStatus.OK)) {
                return responseProfesores;
            }

            // Asignar estudiantes al curso
            ResponseEntity<?> responseEstudiantes = añadirEstudiantes(curso, cursoRequest.getEstudiantes());
            if (!responseEstudiantes.getStatusCode().equals(HttpStatus.OK)) {
                return responseEstudiantes;
            }

            // Crear evaluaciones
            ResponseEntity<?> responseEvaluaciones = crearEvaluaciones(curso, cursoRequest.getPeriodosEvaluacion());
            if (!responseEvaluaciones.getStatusCode().equals(HttpStatus.OK)) {
                return responseEvaluaciones;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Curso creado exitosamente");
            response.put("cursoId", curso.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el curso: " + e.getMessage());
        }
    }

    //Crear evaluaciones
    public ResponseEntity<?> crearEvaluaciones(Curso curso, List<EvaluacionRequest> evaluacionRequests) {
    try {
        // Validar si las fechas de las evaluaciones son consistentes
        for (EvaluacionRequest evaluacionRequest : evaluacionRequests) {
            if (evaluacionRequest.getFechaFin().isBefore(evaluacionRequest.getFechaInicio())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("La fecha de fin no puede ser anterior a la fecha de inicio en las evaluaciones.");
            }
        }

        // Crear las evaluaciones y asociarlas al curso
        List<Evaluacion> evaluaciones = evaluacionRequests.stream()
        .map(evaluacionRequest -> Evaluacion.builder()
                .fechaInicio(evaluacionRequest.getFechaInicio())
                .fechaFin(evaluacionRequest.getFechaFin())
                .curso(curso)
                .build()
        )
        .toList();
        // Guardar todas las evaluaciones en la base de datos
        evaluacionRepository.saveAll(evaluaciones);

        return ResponseEntity.ok("Evaluaciones creadas correctamente.");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al crear las evaluaciones: " + e.getMessage());
    }
}

    //Cambiar estado curso
    public ResponseEntity<?> cambiarEstadoCurso(CursoRequest cursoRequest) {
        try {
            Optional<Curso> cursoOpt = Optional.empty();
            if (cursoRequest.getId() != null && cursoRequest.getId() != 0) {
                cursoOpt = cursoRepository.findById(cursoRequest.getId());
            } else {
                cursoOpt = cursoRepository.findByNombreAsignaturaAndAñoInicioAndCuatrimestreAndActivo(
                        cursoRequest.getNombreAsignatura(), cursoRequest.getAñoInicio(), cursoRequest.getCuatrimestre(), true);
            }

            if (cursoOpt.isPresent()) {
                Curso curso = cursoOpt.get();
                curso.setActivo(!curso.isActivo());
                cursoRepository.save(curso);

                String estado = curso.isActivo() ? "activado" : "desactivado";
                return ResponseEntity.ok("Curso " + estado + " exitosamente");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Curso no encontrado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al cambiar el estado del curso: " + e.getMessage());
        }
    }

    //verificar curso igual ya existente
    public ResponseEntity<?> verificarCursoExistente(CursoRequest cursoRequest) {
        try {
            Optional<Curso> cursoOpt = cursoRepository.findByNombreAsignaturaAndAñoInicioAndCuatrimestreAndActivo(
                    cursoRequest.getNombreAsignatura(), cursoRequest.getAñoInicio(), cursoRequest.getCuatrimestre(), true);

            if (cursoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Curso activo ya existente. ¿Desea desactivarlo?");
            }
            return ResponseEntity.ok("No hay conflicto");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al verificar el curso existente: " + e.getMessage());
        }
    }

    // Obtener cursos de un profesor
    public List<CursoSummaryDTO> obtenerCursosProfesor(String profesorEmail) {
        Profesor profesor = (Profesor) usuarioRepository.findByCorreo(profesorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));

        // Obtener la lista de cursos del profesor
        List<ProfesorCurso> profesorCursos = profesorCursoRepository.findByProfesorId(profesor.getId());
        
        return profesorCursos.stream()
                .map(profesorCurso -> {
                    Curso curso = profesorCurso.getCurso();

                    // Número de estudiantes en el curso
                    int numeroEstudiantes = estudianteCursoRepository.countByCursoId(curso.getId());
                    
                    // Número de equipos en el curso
                    int numeroEquipos = equipoRepository.countByCursoId(curso.getId());
                    
                    // Estudiantes sin equipo
                    List<EstudianteCurso> estudiantesCurso = estudianteCursoRepository.findByCursoId(curso.getId());
                    List<Integer> estudiantesConEquipoIds = estudianteEquipoRepository.findByCursoId(curso.getId()).stream()
                            .map(estudianteEquipo -> estudianteEquipo.getEstudiante().getId())
                            .toList();
                    int numeroEstudiantesSinEquipo = (int) estudiantesCurso.stream()
                            .map(EstudianteCurso::getEstudiante)
                            .filter(estudiante -> !estudiantesConEquipoIds.contains(estudiante.getId()))
                            .count();

                    // Retornar el DTO con los datos calculados
                    return new CursoSummaryDTO(
                            curso.getId(),
                            curso.getNombreAsignatura(),
                            curso.getAñoInicio(),
                            curso.getCuatrimestre(),
                            curso.isActivo(),
                            numeroEstudiantes,
                            numeroEquipos,
                            numeroEstudiantesSinEquipo,
                            curso.getGestionTareas()
                    );
                })
                .toList();
    }


    // Obtener detalle de un curso
    public CursoDetalleDTO obtenerDetalleCurso(Integer cursoId, String profesorEmail) {
        // Verificar que el profesor existe
        Profesor profesor = (Profesor) usuarioRepository.findByCorreo(profesorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado"));

        // Verificar que el profesor tiene acceso al curso
        boolean accesoPermitido = profesorCursoRepository.findByProfesorIdAndCursoId(profesor.getId(), cursoId).isPresent();
        if (!accesoPermitido) {
            throw new IllegalArgumentException("No tiene acceso a este curso.");
        }

        // Obtener el curso
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado"));

        // Obtener estudiantes del curso
        List<EstudianteCurso> estudiantesCurso = estudianteCursoRepository.findByCursoId(cursoId);
        List<Integer> estudiantesConEquipoIds = estudianteEquipoRepository.findByCursoId(cursoId).stream()
                .map(estudianteEquipo -> estudianteEquipo.getEstudiante().getId())
                .toList();

        // Estudiantes sin grupo
        List<EstudianteDTO> estudiantesSinGrupo = estudiantesCurso.stream()
        .filter(estudianteCurso -> !estudiantesConEquipoIds.contains(estudianteCurso.getEstudiante().getId()))
        .map(estudianteCurso -> new EstudianteDTO(
                estudianteCurso.getEstudiante().getId(),
                estudianteCurso.getEstudiante().getNombre(),
                estudianteCurso.getEstudiante().getCorreo(),
                Optional.ofNullable(estudianteCurso.getGrupo())
                            .orElse("N/A")
        ))
        .toList();

        // Equipos con miembros
        List<EquipoDTO> equiposConMiembros = equipoRepository.findByCursoId(cursoId).stream()
        .map(equipo -> new EquipoDTO(
                equipo.getNombre(),
                equipo.getId(),
                equipo.getEvaluador() != null ? equipo.getEvaluador().getId() : null,
                equipo.getGitOrganizacion() != null,
                estudianteEquipoRepository.findByEquipoId(equipo.getId()).stream()
                        .collect(Collectors.toMap(
                                estudianteEquipo -> estudianteEquipo.getEstudiante().getNombre(),
                                estudianteEquipo -> estudiantesCurso.stream()
                                        .filter(ec -> ec.getEstudiante().getId() == estudianteEquipo.getEstudiante().getId())
                                        .findFirst()
                                        .map(EstudianteCurso::getGrupo)
                                        .orElse("N/A")
                        ))
        ))
        .toList();


        // Nombres de los profesores
        List<String> nombresProfesores = profesorCursoRepository.findByCursoId(cursoId).stream()
                .map(profesorCurso -> profesorCurso.getProfesor().getNombre())
                .toList();

        // Crear y retornar el DTO
        String tokenGitHub = null;
        if(curso.getTokenGithubAsignatura() != null){
            tokenGitHub = "********";
        }
        return new CursoDetalleDTO(
                curso.getId(),
                curso.getNombreAsignatura(),
                curso.getAñoInicio(),
                curso.getCuatrimestre(),
                curso.isActivo(),
                curso.getGithubAsignatura(),
                tokenGitHub,
                estudiantesSinGrupo.stream().map(EstudianteDTO::getNombre).toList(),
                estudiantesSinGrupo.stream().map(EstudianteDTO::getCorreo).toList(),
                estudiantesSinGrupo.stream().map(EstudianteDTO::getGrupo).toList(),
                nombresProfesores,
                equiposConMiembros,
                curso.getGestionTareas()
        );
    }


    //Modificar datos basicos curso
    @Transactional
    public ResponseEntity<?> modificarDatosCurso(Curso cursoExistente, CursoRequest cursoRequest) throws Exception {
        if (!cursoExistente.getNombreAsignatura().equals(cursoRequest.getNombreAsignatura()) ||
            cursoExistente.getAñoInicio() != cursoRequest.getAñoInicio() ||
            cursoExistente.getCuatrimestre() != cursoRequest.getCuatrimestre()) {
    
            Optional<Curso> cursoConflicto = cursoRepository.findByNombreAsignaturaAndAñoInicioAndCuatrimestreAndActivo(
                cursoRequest.getNombreAsignatura(), cursoRequest.getAñoInicio(), cursoRequest.getCuatrimestre(), true
            );
    
            if (cursoConflicto.isPresent() && cursoConflicto.get().getId() != (cursoExistente.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un curso activo con el mismo nombre, año y cuatrimestre.");
            }
        }
    
        // Actualizar los datos básicos
        cursoExistente.setNombreAsignatura(cursoRequest.getNombreAsignatura());
        cursoExistente.setAñoInicio(cursoRequest.getAñoInicio());
        cursoExistente.setCuatrimestre(cursoRequest.getCuatrimestre());
        cursoExistente.setGithubAsignatura(cursoRequest.getGithubAsignatura());

        if(cursoExistente.getGestionTareas().equals("Taiga") && cursoRequest.getGestionTareas().equals("GitHub")){
            //Cambiar la gestión de tareas del curso
            cursoExistente.setGestionTareas(cursoRequest.getGestionTareas());

            //Obtener los equipos del curso y desconectarlos de Taiga
            List <Equipo> equipos = cursoExistente.getEquipos();
            if (equipos != null && !equipos.isEmpty()) {
                for (Equipo equipo : equipos) {
                    taigaService.desconectarProyecto(equipo.getId());
                }
            }
        }else{
            cursoExistente.setGestionTareas(cursoRequest.getGestionTareas());
        }


        String tokenRecibido = cursoRequest.getTokenGithubAsignatura();

        //Nos aseguramos de que no sea nulo y no sea la máscara "********"
        if (tokenRecibido != null && !tokenRecibido.equals("********")) {

            // 2. Si viene vacío, asumimos que el usuario quiere borrar el token
            if (tokenRecibido.isEmpty()) {
                cursoExistente.setTokenGithubAsignatura(null);
            }
            // 3. Si tiene texto de verdad, lo encriptamos y lo guardamos
            else {
                String encryptedToken = tokenEncrypter.encrypt(tokenRecibido);
                cursoExistente.setTokenGithubAsignatura(encryptedToken);
            }
        }
    
        return ResponseEntity.ok().build();
    }

    // Función para añadir estudiantes al curso
    public ResponseEntity<?> añadirEstudiantes(Curso cursoExistente, List<EstudianteRequest> estudiantesAñadir) {
        for (EstudianteRequest estudianteRequest : estudiantesAñadir) {
            Usuario usuarioExistente = usuarioRepository.findByCorreo(estudianteRequest.getCorreo()).orElse(null);
            if (usuarioExistente == null) {
                // Llamar al servicio para crear el usuario si todavia no existe
                ResponseEntity<?> response = usuarioService.crearUsuario(estudianteRequest.getNombre(), estudianteRequest.getCorreo(), Rol.Estudiante);

                if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                    usuarioExistente = (Usuario) response.getBody();
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error al crear el estudiante: " + estudianteRequest.getCorreo());
                }

            }
            // Asociar al estudiante con el curso
            EstudianteCurso estudianteCurso = new EstudianteCurso((Estudiante) usuarioExistente, cursoExistente, estudianteRequest.getGrupo());
            estudianteCursoRepository.save(estudianteCurso);
        }
        return ResponseEntity.ok().build();
    }

    // Función para borrar estudiantes del curso
    public void borrarEstudiantes(Curso cursoExistente, List<EstudianteRequest> estudiantesBorrar) {
        for (EstudianteRequest estudianteRequest : estudiantesBorrar) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(estudianteRequest.getCorreo());

            if (usuarioOpt.isPresent() && usuarioOpt.get() instanceof Estudiante) {
                Estudiante estudiante = (Estudiante) usuarioOpt.get();

                // Eliminar la relación entre el estudiante y el curso
                Optional<EstudianteCurso> estudianteCursoOpt = estudianteCursoRepository.findByEstudianteAndCurso(estudiante, cursoExistente);
                if (estudianteCursoOpt.isPresent()) {
                    estudianteCursoRepository.delete(estudianteCursoOpt.get());
                }

                // Verificar si el estudiante no está en ningún otro curso
                List<EstudianteCurso> cursosDelEstudiante = estudianteCursoRepository.findByEstudianteId(estudiante.getId());
                if (cursosDelEstudiante.isEmpty()) {
                    usuarioRepository.delete(estudiante);
                }
            }
        }
    }

    // Función para añadir profesores al curso
    public ResponseEntity<?> añadirProfesores(Curso cursoExistente, List<ProfesorRequest> profesoresAñadir) {
        for (ProfesorRequest profesorRequest : profesoresAñadir) {
            Usuario usuarioExistente = usuarioRepository.findByCorreo(profesorRequest.getCorreo()).orElse(null);
            
            if (usuarioExistente == null) {
                // Crear un nuevo profesor si no existe
                ResponseEntity<?> response = usuarioService.crearUsuario(profesorRequest.getNombre(), profesorRequest.getCorreo(), Rol.Profesor);

                if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                    usuarioExistente = (Usuario) response.getBody();
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error al crear el profesor: " + profesorRequest.getCorreo());
                }
            }

            // Crear la relación ProfesorCurso
            ProfesorCurso profesorCurso = new ProfesorCurso((Profesor) usuarioExistente, cursoExistente);
            profesorCursoRepository.save(profesorCurso);
        
        }
        return ResponseEntity.ok().build();
    }


    // Función para borrar profesores del curso
    public void borrarProfesores(Curso cursoExistente, List<ProfesorRequest> profesoresBorrar) {
        List<Integer> idsProfesoresBorrados = new ArrayList<>();

        for (ProfesorRequest profesorRequest : profesoresBorrar) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(profesorRequest.getCorreo());
            if (usuarioOpt.isPresent() && usuarioOpt.get() instanceof Profesor) {
                Profesor profesor = (Profesor) usuarioOpt.get();

                // Buscar la relación ProfesorCurso y eliminarla si existe
                Optional<ProfesorCurso> profesorCursoOpt = profesorCursoRepository.findByProfesorIdAndCursoId(profesor.getId(), cursoExistente.getId());
                if (profesorCursoOpt.isPresent()) {
                    profesorCursoRepository.delete(profesorCursoOpt.get());
                    
                    // Agregar el ID del profesor a la lista de IDs a borrar del curso
                    idsProfesoresBorrados.add(profesor.getId());
                }
            } else {
                System.out.println("El profesor con correo " + profesorRequest.getCorreo() + " no fue encontrado o no es un profesor.");
            }
        }

        // Actualizar la lista de profesores del curso eliminando todos los profesores borrados
        cursoExistente.getProfesores().removeIf(profesor -> idsProfesoresBorrados.contains(profesor.getId()));
    }
    

    public Map<String, List<EstudianteDTO>> obtenerEstudiantesPorCurso(int cursoId) {
        // Obtener todos los estudiantes del curso
        List<EstudianteCurso> estudiantesCurso = estudianteCursoRepository.findByCursoId(cursoId);
    
        // Crear listas separadas para estudiantes con equipo y sin equipo
        List<EstudianteDTO> estudiantesConEquipo = new ArrayList<>();
        List<EstudianteDTO> estudiantesSinEquipo = new ArrayList<>();
    
        for (EstudianteCurso ec : estudiantesCurso) {
            int estudianteId = ec.getEstudiante().getId();
            boolean tieneEquipo = !estudianteEquipoRepository.findByEstudianteIdAndCursoId(estudianteId, cursoId).isEmpty();
    
            String nombre = ec.getEstudiante().getNombre();
            String correo = ec.getEstudiante().getCorreo();
    
            EstudianteDTO estudianteDTO = new EstudianteDTO(estudianteId, nombre, correo, null);
    
            if (tieneEquipo) {
                estudiantesConEquipo.add(estudianteDTO);
            } else {
                estudiantesSinEquipo.add(estudianteDTO);
            }
        }
    
        // Crear el mapa de respuesta
        Map<String, List<EstudianteDTO>> respuesta = new HashMap<>();
        respuesta.put("conEquipo", estudiantesConEquipo);
        respuesta.put("sinEquipo", estudiantesSinEquipo);
    
        return respuesta;
    }


    public List<EstudianteDTO> obtenerProfesoresDelCurso(int cursoId) {
        // Obtener la lista de profesores del curso
        return profesorCursoRepository.findByCursoId(cursoId).stream()
                .map(profesorCurso -> {
                    Profesor profesor = profesorCurso.getProfesor();
                    return new EstudianteDTO(
                            profesor.getId(),
                            profesor.getNombre(),
                            profesor.getCorreo(),
                            null
                    );
                })
                .toList();
    }

    public void borrarCurso(int cursoId) {
        // Verificar si el curso existe
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado."));

        // Eliminar el curso
        cursoRepository.delete(curso);
    }



}
