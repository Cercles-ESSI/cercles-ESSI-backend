package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipoDTO {
    private String nombreEquipo;
    private int id_equipo;
    private int idProfe;
    private boolean validado;
    private Map<String,String> miembros;
    private Map<String,String> correos;

}
