package tfg.backend_tfg.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@Table(name = "historias_equipo")

public class HistoriasUsuarioEquipo {
    @Id
    @Column(name = "id_historia")
    private Integer id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String estado;

    @Column
    private String sprint;

    @Column(nullable = false)
    private Integer puntosEsfuerzo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Estudiante responsable;

}
