package tfg.backend_tfg.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import tfg.backend_tfg.model.Profesor;
import tfg.backend_tfg.model.Rol;
import tfg.backend_tfg.model.Usuario;
import tfg.backend_tfg.repository.UsuarioRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.initial-email}")
    private String firstUser;

    @Value("${app.initial-name}")
    private String firstUserName;


    @Override
    public void run(String... args) throws Exception {
        // 1. Verificamos que la BD esté vacía
        if (usuarioRepository.count() == 0) {

            // 2. Validamos que el correo Y nombre no sea nulo, vacío ni la plantilla por defecto
            if (firstUser == null || firstUser.trim().isEmpty() || firstUser.contains("<") && (firstUserName == null || firstUserName.trim().isEmpty() || firstUserName.contains("<"))) {
                System.err.println("ADVERTENCIA: La base de datos está vacía pero no se ha configurado un correo válido en app.initial-email. No se creará el usuario.");
                return;
            }

            // 3. Creamos el usuario
            Usuario usuario;
            Rol rolUser = Rol.Profesor;
            String taigaUsername = firstUser.substring(0, firstUser.indexOf("@"));
            usuario = Profesor.builder()
                    .correo(firstUser)
                    .nombre(firstUserName)
                    .taigaUsername(taigaUsername)
                    .rol(rolUser)
                    .build();

            usuarioRepository.save(usuario);
            System.out.println("ÉXITO: Usario inicial creado con el correo: " + firstUser);
        }
    }
}