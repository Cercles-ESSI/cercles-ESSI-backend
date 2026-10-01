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
    private String org;
    private String TaigaPrj;
    private Map<String,String> miembros;
    private Map<String, String> usuariosGithub;
    private Map<String, String> usuariosTaiga;

}
