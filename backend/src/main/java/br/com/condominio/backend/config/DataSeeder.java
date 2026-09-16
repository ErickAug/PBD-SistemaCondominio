package br.com.condominio.backend.config;

import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoCondominio;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final AdministradoraRepository administradoraRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AdministradoraRepository administradoraRepository,
            CondominioRepository condominioRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.administradoraRepository = administradoraRepository;
        this.condominioRepository = condominioRepository;
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

        Usuario usuarioAdmin = new Usuario();
        usuarioAdmin.setNome("Admin Master");
        usuarioAdmin.setUsuario("admin");
        usuarioAdmin.setSenha(passwordEncoder.encode("admin123"));
        usuarioAdmin.setPerfil(Perfil.ADMINISTRADORA);
        usuarioAdmin.setAdministradora(administradora);
        usuarioRepository.save(usuarioAdmin);

        Condominio condominio = new Condominio();
        condominio.setNome("Residencial Teste");
        condominio.setEndereco("Rua Exemplo, 123");
        condominio.setCnpj("00000000000272");
        condominio.setSituacao(SituacaoCondominio.ATIVO);
        condominio.setAdministradora(administradora);
        condominioRepository.save(condominio);

        Usuario usuarioSindico = new Usuario();
        usuarioSindico.setNome("Síndico Teste");
        usuarioSindico.setUsuario("sindico");
        usuarioSindico.setSenha(passwordEncoder.encode("sindico123"));
        usuarioSindico.setPerfil(Perfil.SINDICO);
        usuarioSindico.setCondominio(condominio);
        usuarioRepository.save(usuarioSindico);

        System.out.println("Usuario inicial criado: login=admin / senha=admin123");
        System.out.println(
                "Usuario sindico criado: login=sindico / senha=sindico123 / condominioId=" + condominio.getId());
    }
}