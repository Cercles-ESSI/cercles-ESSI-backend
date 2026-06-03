package tfg.backend_tfg.model;

import java.util.List;

public class CursoRequest {
    private int id;
    private String nombreAsignatura;
    private int añoInicio;
    private int cuatrimestre;
    private boolean activo;
    private String githubAsignatura;
    private String tokenGithubAsignatura;
    private String gestionTareas;
    private List<EstudianteRequest> estudiantes;
    private List<ProfesorRequest> profesores;

    private List<EstudianteRequest> estudiantesAñadir;
    private List<EstudianteRequest> estudiantesBorrar;

    private List<ProfesorRequest> profesoresAñadir;
    private List<ProfesorRequest> profesoresBorrar;

    private List<EvaluacionRequest> periodosEvaluacion;

    // Getters y setters
    public Integer getId() {
        return id;
    }
    
    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public void setNombreAsignatura(String nombreAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
    }

    public String getGestionTareas() {
        return gestionTareas;
    }

    public void setGestionTareas(String gestionTareas) {
        this.gestionTareas = gestionTareas;
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

    public String getGithubAsignatura () {
        return githubAsignatura;
    }

    public void setGithubAsignatura(String githubAsignatura) {
        this.githubAsignatura = githubAsignatura;
    } 
    
    public String getTokenGithubAsignatura() {
        return tokenGithubAsignatura;
    }

    public void setTokenGithubAsignatura(String tokenGithubAsignatura) {
        this.tokenGithubAsignatura = tokenGithubAsignatura;
    }

    public List<EstudianteRequest> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(List<EstudianteRequest> estudiantes) {
        this.estudiantes = estudiantes;
    }

    public List<ProfesorRequest> getProfesores() {
        return profesores;
    }

    public void setProfesores(List<ProfesorRequest> profesores) {
        this.profesores = profesores;
    }

    public List<EstudianteRequest> getEstudiantesAñadir(){
        return estudiantesAñadir;
    }

    public void setEstudiantesAñadir(List<EstudianteRequest> estudiantesAñadir) {
        this.estudiantesAñadir = estudiantesAñadir;
    }

    public List<EstudianteRequest> getEstudiantesBorrar(){
        return estudiantesBorrar;
    }

    public void setEstudiantesBorrar(List<EstudianteRequest> estudiantesBorrar) {
        this.estudiantesBorrar = estudiantesBorrar;
    }

    public List<ProfesorRequest> getProfesoresAñadir() {
        return profesoresAñadir;
    }

    public void getProfesoresAñadir(List<ProfesorRequest> profesoresAñadir) {
        this.profesoresAñadir = profesoresAñadir;
    }

    public List<ProfesorRequest> getProfesoresBorrar() {
        return profesoresBorrar;
    }

    public void getProfesoresBorrar(List<ProfesorRequest> profesoresBorrar) {
        this.profesoresBorrar = profesoresBorrar;
    }

    public List<EvaluacionRequest> getPeriodosEvaluacion() {
        return periodosEvaluacion;
    }

    public void setPeriodosEvaluacion(List<EvaluacionRequest> periodosEvaluacion) {
        this.periodosEvaluacion = periodosEvaluacion;
    }
}
