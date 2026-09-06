package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipoDetalleDTO {
    private int id;
    private String nombre;

    // Información del curso
    private int cursoId;
    private String nombreAsignatura;
    private int añoInicio;
    private int cuatrimestre;
    private boolean activo;
    private String githubAsignatura;
    private String tokenGithub;
    private String gestionTareas;

    // Información del evaluador
    private int evaluadorId;
    private String evaluadorNombre;
    private String evaluadorCorreo;
    private String taigaUserProf;


    // Lista de estudiantes
    private List<EstudianteDTO> estudiantes;

    //Información de github
    private String gitOrganizacion;
    private LocalDateTime ultimaSincronizacionGit;

    // Información de Taiga
    private String taigaProyecto;
    private LocalDateTime ultimaSincronizacionTareas;
    private LocalDateTime ultimaSincronizacionHistorias;



/*
    // Constructor completo
    public EquipoDetalleDTO(int id, String nombre, int cursoId, String nombreAsignatura,
                                    int añoInicio, int cuatrimestre, boolean activo, String githubAsignatura, String tokenGithub,String gestionTareas,
                                    int evaluadorId, String evaluadorNombre, String evaluadorCorreo, String taigaUserProf,
                                    List<EstudianteDTO> estudiantes, String gitOrganizacion, String taigaProyecto, LocalDateTime ultimaSincronizacionGit,LocalDateTime ultimaSincronizacionTareas, LocalDateTime ultimaSincronizacionHistorias) {
        this.id = id;
        this.nombre = nombre;
        this.cursoId = cursoId;
        this.nombreAsignatura = nombreAsignatura;
        this.añoInicio = añoInicio;
        this.cuatrimestre = cuatrimestre;
        this.activo = activo;
        this.githubAsignatura = githubAsignatura;
        this.tokenGithub = tokenGithub;
        this.gestionTareas = gestionTareas;
        this.evaluadorId = evaluadorId;
        this.evaluadorNombre = evaluadorNombre;
        this.evaluadorCorreo = evaluadorCorreo;
        this.taigaUserProf = taigaUserProf;
        this.estudiantes = estudiantes;
        this.gitOrganizacion = gitOrganizacion;
        this.taigaProyecto = taigaProyecto;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getCursoId() {
        return cursoId;
    }

    public void setCursoId(int cursoId) {
        this.cursoId = cursoId;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public void setNombreAsignatura(String nombreAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
    }

    public int getAñoInicio() {
        return añoInicio;
    }

    public void setAñoInicio(int añoInicio) {
        this.añoInicio = añoInicio;
    }

    public int getCuatrimestre() {
        return cuatrimestre;
    }

    public void setCuatrimestre(int cuatrimestre) {
        this.cuatrimestre = cuatrimestre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getGithubAsignatura() {
        return githubAsignatura;
    }

    public void setGithubAsignatura(String githubAsignatura) {
        this.githubAsignatura = githubAsignatura;
    }

    public String getTokenGithub() {
        return tokenGithub;
    }

    public void setTokenGithub(String tokenGithub) {
        this.tokenGithub = tokenGithub;
    }

    public String getGestionTareas() {
        return gestionTareas;
    }

    public void setGestionTareas(String gestionTareas) {
        this.gestionTareas = gestionTareas;
    }

    public int getEvaluadorId() {
        return evaluadorId;
    }

    public void setEvaluadorId(int evaluadorId) {
        this.evaluadorId = evaluadorId;
    }

    public String getEvaluadorNombre() {
        return evaluadorNombre;
    }

    public void setEvaluadorNombre(String evaluadorNombre) {
        this.evaluadorNombre = evaluadorNombre;
    }

    public String getEvaluadorCorreo() {
        return evaluadorCorreo;
    }

    public void setEvaluadorCorreo(String evaluadorCorreo) {
        this.evaluadorCorreo = evaluadorCorreo;
    }

    public String getTaigaUserProf() {
        return taigaUserProf;
    }

    public void setTaigaUserProf(String taigaUserProf) {
        this.taigaUserProf = taigaUserProf;
    }

    public List<EstudianteDTO> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(List<EstudianteDTO> estudiantes) {
        this.estudiantes = estudiantes;
    }

    public String getGitOrganizacion() {
        return gitOrganizacion;
    }

    public void setGitOrganizacion(String gitOrganizacion) {
        this.gitOrganizacion = gitOrganizacion;
    }

    public String getTaigaProyecto() {
        return taigaProyecto;
    }

    public void setTaigaProyecto(String taigaProyecto) {
        this.taigaProyecto = taigaProyecto;
    }
*/
}
