package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@Table(name = "curso")
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nombre_asignatura", nullable = false)
    private String nombreAsignatura;

    @Column(name = "año_inicio", nullable = false)
    private int añoInicio;

    @Column(nullable = false)
    private int cuatrimestre;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "github_asignatura", nullable = false)
    private String githubAsignatura;

    @Column(name = "token_github_asignatura", nullable = false)
    private String tokenGithubAsignatura;

    @Column(name = "gestion_tareas", nullable = false)
    private String gestionTareas;

    // Relación con profesores a través de ProfesorCurso
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfesorCurso> profesores;

    // Relación muchos a muchos con estudiantes a través de EstudianteCurso
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstudianteCurso> estudiantes;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipo> equipos;

    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Evaluacion> evaluaciones;
}
