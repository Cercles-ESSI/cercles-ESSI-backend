package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private Profesor evaluador;

    @Column(name = "git_organizacion")
    private String gitOrganizacion;

    @Column(name = "taiga_proyecto")
    private String taigaProyecto;

    @Column(name = "taiga_project_id")
    private Integer taigaProyectoId;

    @OneToMany
    @JoinTable(
            name = "estudiante_equipo",
            joinColumns = @JoinColumn(name = "id_equipo"),
            inverseJoinColumns = @JoinColumn(name = "id_estudiante") // La columna que apunta a la id de Estudiante
    )
    private List<Estudiante> estudiantes;


    @Column(name = "ultima_sync_historias")
    private LocalDateTime ultimaSincronizacionHistorias;

    @Column(name = "ultima_sync_tareas")
    private LocalDateTime ultimaSincronizacionTareas;

}
