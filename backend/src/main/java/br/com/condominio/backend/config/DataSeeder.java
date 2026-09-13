package br.com.condominio.backend.config;

import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final AdministradoraRepository administradoraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AdministradoraRepository administradoraRepository,
                      UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder) {
        this.administradoraRepository = administradoraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (administradoraRepository.count() > 0) {
            return;
        }

        Administradora administradora = new Administradora();
        administradora.setNome("Administradora Teste");
        administradora.setCnpj("00000000000191");
        administradoraRepository.save(administradora);

        Usuario usuario = new Usuario();
        usuario.setNome("Admin Master");
        usuario.setUsuario("admin");
        usuario.setSenha(passwordEncoder.encode("admin123"));
        usuario.setPerfil(Perfil.ADMINISTRADORA);
        usuario.setAdministradora(administradora);
        usuarioRepository.save(usuario);

        System.out.println("Usuario inicial criado: login=admin / senha=admin123");
    }
}