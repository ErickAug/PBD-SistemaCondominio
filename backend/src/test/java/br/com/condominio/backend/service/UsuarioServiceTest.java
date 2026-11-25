package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AdministradoraRepository administradoraRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Condominio condominio() {
        Condominio condominio = new Condominio();
        condominio.setId(1L);
        condominio.setNome("Condomínio Teste");
        return condominio;
    }

    private Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome("Fulano");
        usuario.setUsuario("fulano" + perfil.name().toLowerCase());
        usuario.setSenha("123456");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"MORADOR", "PROPRIETARIO", "PORTARIA"})
    void sindico_pode_cadastrar_morador_proprietario_e_portaria(Perfil perfil) {
        lenient().when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertDoesNotThrow(() ->
                usuarioService.cadastrarNoCondominio(usuario(perfil), condominio(), Perfil.SINDICO));
    }

    @Test
    void sindico_nao_pode_cadastrar_outro_sindico() {
        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> usuarioService.cadastrarNoCondominio(usuario(Perfil.SINDICO), condominio(), Perfil.SINDICO));

        assertTrue(excecao.getMessage().contains("Síndico só pode cadastrar"));
    }

    @Test
    void administradora_pode_cadastrar_sindico() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertDoesNotThrow(() ->
                usuarioService.cadastrarNoCondominio(usuario(Perfil.SINDICO), condominio(), Perfil.ADMINISTRADORA));
    }
}
