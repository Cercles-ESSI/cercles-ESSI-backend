package tfg.backend_tfg.services;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tfg.backend_tfg.dto.*;
import tfg.backend_tfg.model.Curso;
import tfg.backend_tfg.model.Equipo;
import tfg.backend_tfg.model.Estudiante;
import tfg.backend_tfg.model.EstudianteEquipo;
import tfg.backend_tfg.model.Evaluacion;
import tfg.backend_tfg.model.EvaluacionDetalle;
import tfg.backend_tfg.repository.EquipoRepository;
import tfg.backend_tfg.repository.EstudianteEquipoRepository;
import tfg.backend_tfg.repository.EstudianteRepository;
import tfg.backend_tfg.repository.EvaluacionDetalleRepository;
import tfg.backend_tfg.repository.EvaluacionRepository;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EvaluacionService {

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private EvaluacionDetalleRepository detalleRepository;

    @Autowired
    private EstudianteEquipoRepository estudianteEquipoRepository;

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private EvaluacionDetalleRepository evaluacionDetalleRepository;


    public boolean existsByEvaluacionIdAndEvaluadorId(int evaluacionId, int estudianteId) {
        return evaluacionDetalleRepository.existsByEvaluacionIdAndEvaluadorId(evaluacionId, estudianteId);
    }

    public Evaluacion crearEvaluacion(CrearEvaluacionDTO crearEvaluacionDTO) {
        Evaluacion evaluacion = Evaluacion.builder()
                .fechaInicio(crearEvaluacionDTO.getFechaInicio())
                .fechaFin(crearEvaluacionDTO.getFechaFin())
                .curso(Curso.builder().id(crearEvaluacionDTO.getCursoId()).build())
                .build();
        return evaluacionRepository.save(evaluacion);
    }

    @Transactional
    public void crearEvaluacionDetalle(Integer evaluacionId, Integer evaluadorId, List<EvaluacionDetalleDTO> detalles) {
        // Verificar equipo del evaluador
        EstudianteEquipo evaluadorEquipo = estudianteEquipoRepository
                .findByEstudianteIdAndEquipoId(evaluadorId, detalles.get(0).getEquipoId())
                .orElseThrow(() -> new IllegalArgumentException("El evaluador no está asignado a este equipo"));
    
        Equipo equipo = evaluadorEquipo.getEquipo();
    
        // Validar que todos los evaluados pertenezcan al mismo equipo y que la suma de puntos sea correcta
        int totalPuntos = 0;
    
        for (EvaluacionDetalleDTO detalle : detalles) {
            boolean perteneceAlEquipo = estudianteEquipoRepository.existsByEstudianteIdAndEquipoId(detalle.getEvaluadoId(), equipo.getId());
            if (!perteneceAlEquipo) {
                throw new IllegalArgumentException("El evaluado no pertenece al mismo equipo que el evaluador");
            }
    
            totalPuntos += detalle.getPuntos();
        }
    
        // Verificar el total de puntos permitidos
        long maxPuntos = estudianteEquipoRepository.countByEquipoId(equipo.getId()) * 10;
        if (totalPuntos != maxPuntos) {
            throw new IllegalArgumentException(
                    "La suma de puntos asignados no coincide con el máximo permitido (" + maxPuntos + ")");
        }
    
        // Guardar los detalles de la evaluación
        for (EvaluacionDetalleDTO detalle : detalles) {
            EvaluacionDetalle evaluacionDetalle = EvaluacionDetalle.builder()
                    .evaluacion(Evaluacion.builder().id(evaluacionId).build())
                    .evaluador(Estudiante.builder().id(evaluadorId).build())
                    .evaluado(Estudiante.builder().id(detalle.getEvaluadoId()).build())
                    .equipo(equipo)
                    .puntos(detalle.getPuntos())
                    .build();
    
            detalleRepository.save(evaluacionDetalle);
        }
    }

    public List<EvaluacionResumenDTO> obtenerEvaluacionesPorEquipo(Integer equipoId, List<Integer> evaluacionIds) {
        List<EvaluacionDetalle> detalles;
    
        // Si se especifican evaluaciones, filtramos por ellas
        if (evaluacionIds != null && !evaluacionIds.isEmpty()) {
            detalles = detalleRepository.findByEquipoIdAndEvaluacionIdIn(equipoId, evaluacionIds);
        } else {
            detalles = detalleRepository.findByEquipoId(equipoId);
        }
    
        // Agrupar los detalles por estudiante evaluado
        Map<Integer, List<EvaluacionDetalle>> agrupadosPorEvaluado = detalles.stream()
                .collect(Collectors.groupingBy(detalle -> detalle.getEvaluado().getId()));
    
        List<EvaluacionResumenDTO> resultado = new ArrayList<>();
    
        for (Map.Entry<Integer, List<EvaluacionDetalle>> entry : agrupadosPorEvaluado.entrySet()) {
            Integer evaluadoId = entry.getKey();
    
            // Agrupar las evaluaciones por evaluacionId
            Map<Integer, List<EvaluacionDetalle>> agrupadosPorEvaluacionId = entry.getValue().stream()
                    .collect(Collectors.groupingBy(detalle -> detalle.getEvaluacion().getId()));
    
            List<EvaluacionPorEvaluacionIdDTO> evaluaciones = new ArrayList<>();
            for (Map.Entry<Integer, List<EvaluacionDetalle>> evalEntry : agrupadosPorEvaluacionId.entrySet()) {
                Integer evaluacionId = evalEntry.getKey();
                List<EvaluacionDTO> detallesEvaluacion = evalEntry.getValue().stream()
                        .map(detalle -> new EvaluacionDTO(detalle.getEvaluador().getId(), detalle.getPuntos()))
                        .collect(Collectors.toList());
    
                evaluaciones.add(new EvaluacionPorEvaluacionIdDTO(evaluacionId, detallesEvaluacion));
            }
    
            resultado.add(new EvaluacionResumenDTO(evaluadoId, evaluaciones));
        }
    
        return resultado;
    }

    public List<MiAutoEvaluacionDTO> obtenerMisAutoEvaluaciones(Integer equipoId, Integer estudianteId) {

        List<EvaluacionDetalle> misDetalles = detalleRepository.findByEquipoIdAndEvaluadoIdAndEvaluadorId(
                equipoId, estudianteId, estudianteId
        );

        // Mapeamos el resultado al nuevo DTO simple
        return misDetalles.stream()
                .map(detalle -> new MiAutoEvaluacionDTO(
                        detalle.getEvaluacion().getId(),
                        detalle.getPuntos()
                ))
                .collect(Collectors.toList());
    }
    

    public List<EvaluacionMediaDTO> obtenerMediasPorEquipo(Integer equipoId) {
        List<EvaluacionDetalle> detalles = detalleRepository.findByEquipoId(equipoId);
    
        // Agrupar detalles por estudiante evaluado
        Map<Integer, List<EvaluacionDetalle>> agrupadosPorEvaluado = detalles.stream()
                .collect(Collectors.groupingBy(detalle -> detalle.getEvaluado().getId()));
    
        List<EvaluacionMediaDTO> resultado = new ArrayList<>();
    
        // Calcular medias por estudiante
        for (Map.Entry<Integer, List<EvaluacionDetalle>> entry : agrupadosPorEvaluado.entrySet()) {
            Integer estudianteId = entry.getKey();
            List<EvaluacionDetalle> evaluaciones = entry.getValue();
    
            // Agrupar por evaluación
            Map<Integer, List<EvaluacionDetalle>> agrupadosPorEvaluacion = evaluaciones.stream()
                    .collect(Collectors.groupingBy(detalle -> detalle.getEvaluacion().getId()));
    
            List<MediaPorEvaluacionDTO> mediasPorEvaluacion = new ArrayList<>();
            double sumaMediasCompañeros = 0;
            double sumaNotasPropias = 0;
            int totalEvaluaciones = agrupadosPorEvaluacion.size();
    
            for (Map.Entry<Integer, List<EvaluacionDetalle>> evalEntry : agrupadosPorEvaluacion.entrySet()) {
                Integer evaluacionId = evalEntry.getKey();
                List<EvaluacionDetalle> detallesEvaluacion = evalEntry.getValue();
    
                double mediaDeCompañeros = detallesEvaluacion.stream()
                        .filter(detalle -> detalle.getEvaluador().getId() != estudianteId)
                        .mapToInt(EvaluacionDetalle::getPuntos)
                        .average()
                        .orElse(0);
    
                int notaPropia = detallesEvaluacion.stream()
                        .filter(detalle -> detalle.getEvaluador().getId() == estudianteId)
                        .mapToInt(EvaluacionDetalle::getPuntos)
                        .findFirst()
                        .orElse(0);
    
                sumaMediasCompañeros += mediaDeCompañeros;
                sumaNotasPropias += notaPropia;
    
                mediasPorEvaluacion.add(new MediaPorEvaluacionDTO(evaluacionId, mediaDeCompañeros, notaPropia));
            }
    
            double mediaGeneralDeCompañeros = sumaMediasCompañeros / totalEvaluaciones;
            double mediaGeneralPropia = sumaNotasPropias / totalEvaluaciones;
    
            resultado.add(new EvaluacionMediaDTO(estudianteId, mediasPorEvaluacion, mediaGeneralDeCompañeros, mediaGeneralPropia));
        }
    
        return resultado;
    }
    
    public Map<String, Object> getEvaluacionActiva(Integer equipoId) {
        Curso curso = equipoRepository.findByEquipoId(equipoId);

        if (curso == null) {
            throw new IllegalArgumentException("El equipo no tiene un curso asociado");
        }

        List<Evaluacion> evaluaciones = evaluacionRepository.findByCursoId(curso.getId());
        LocalDate today = LocalDate.now();

        for (Evaluacion evaluacion : evaluaciones) {
            if (!today.isBefore(evaluacion.getFechaInicio()) && !today.isAfter(evaluacion.getFechaFin())) {
                Map<String, Object> result = new HashMap<>();
                result.put("activa", true);
                result.put("fechaInicio", evaluacion.getFechaInicio());
                result.put("fechaFin", evaluacion.getFechaFin());
                return result;
            }
        }

        // Si no hay evaluación activa, devolver respuesta inactiva
        Map<String, Object> result = new HashMap<>();
        result.put("activa", false);
        return result;
    }

    
    public Integer obtenerEvaluacionActiva(Integer equipoId) {
        Curso curso = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"))
                .getCurso();
    
        LocalDate today = LocalDate.now();
        
        return evaluacionRepository.findByCursoId(curso.getId()).stream()
                .filter(evaluacion -> !today.isBefore(evaluacion.getFechaInicio()) && !today.isAfter(evaluacion.getFechaFin()))
                .map(Evaluacion::getId)
                .findFirst()
                .orElse(0);
    }
    

    public List<Integer> obtenerIdsEvaluacionesPorCurso(Integer cursoId) {
        return evaluacionRepository.findIdsByCursoId(cursoId);
    }


}
