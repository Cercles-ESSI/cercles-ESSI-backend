package tfg.backend_tfg.dto;

import lombok.Data;

import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;

import java.util.List;

@Data

@NoArgsConstructor

@AllArgsConstructor
public class HistoriasEquipoDTO {
    private int totalHistorias;
    private int totalPuntosEsfuerzoEquipo;
    private List<HistoriasEstudianteDTO> metricasEstudiantes;

}
