package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas_equipo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TareasEquipo {

    @Id
    @Column(name = "id_tarea")
    private Integer id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id")
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "historia_usuario_id")
    private HistoriasUsuarioEquipo historiaUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;
}
