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
@Table(
        name = "historias_equipo",
        uniqueConstraints = {
            @UniqueConstraint(
            name = "uk_historia_equipo",
            columnNames = {"id_hisdtoria", "equipo_id"}
            )
        }
)

public class HistoriasUsuarioEquipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_historia")
    private Integer idHistoria;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String estado;

    @Column
    private String sprint;

    @Column
    private Integer puntosEsfuerzo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante responsable;

    @OneToMany(mappedBy = "historiaUsuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TareasEquipo> tareas;

}
