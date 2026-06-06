package tfg.backend_tfg.dto;

import lombok.Data;

import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;

import java.util.List;


@Data

@NoArgsConstructor

@AllArgsConstructor

public class TareasEquipoDTO {
    private int totalTareasEquipo;
    private List<TareasEstudianteDTO> metricasEstudiantes;

}
