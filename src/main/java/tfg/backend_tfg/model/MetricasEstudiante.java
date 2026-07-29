package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@Table(
        name = "metricas_estudiante",
        uniqueConstraints = {
                // Garantiza que solo haya una fila de métricas por cada combinación de Estudiante + Equipo
                @UniqueConstraint(columnNames = {"estudiante_id", "equipo_id"})
        }
)
public class MetricasEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Muchos registros de métricas pueden pertenecer a un mismo estudiante
    @ManyToOne
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    // Muchos registros de métricas pueden pertenecer a un mismo equipo
    @ManyToOne
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    private Integer totalCommits;
    private Integer linesAdded;
    private Integer linesRemoved;
    private Integer pullRequestsMerged;

}
