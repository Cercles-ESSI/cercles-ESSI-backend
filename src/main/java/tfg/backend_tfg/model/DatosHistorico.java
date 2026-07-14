package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "datos_historico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // A qué fase de evaluación pertenece esta foto (Evaluación 1, 2, 3...)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluacion_id")
    private Evaluacion evaluacion;

    // De qué alumno son estos datos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    // Las métricas que extraemos de Taiga/GitHub en ese momento exacto
    private Integer tareasCerradas;
    private Integer tareasAbiertas;
    private Integer storyPointsCompletados;
    private Integer commitsRealizados;
    private Integer lineasModificadas;

    @CreationTimestamp
    @Column(name = "fecha_guardado", updatable = false)
    private LocalDateTime fechaGuardado;

}
