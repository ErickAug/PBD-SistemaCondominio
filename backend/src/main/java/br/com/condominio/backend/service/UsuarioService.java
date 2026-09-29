package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<Perfil> PERFIS_QUE_SINDICO_PODE_CRIAR = Set.of(Perfil.MORADOR, Perfil.PROPRIETARIO);

    private final UsuarioRepository usuarioRepository;
    private final AdministradoraRepository administradoraRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AdministradoraRepository administradoraRepository,
                          UnidadeRepository unidadeRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.administradoraRepository = administradoraRepository;
        this.unidadeRepository = unidadeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrarAdministrador(Usuario usuario, Long administradoraId) {
        if (usuario.getPerfil() != Perfil.ADMINISTRADORA) {
            throw new RegraDeNegocioException("Este endpoint só cria usuários com perfil ADMINISTRADORA.");
        }

        validarLoginDisponivel(usuario.getUsuario());

        Administradora administradora = administradoraRepository.findById(administradoraId)
                .orElseThrow(() -> new RegraDeNegocioException("Administradora não encontrada."));

        usuario.setAdministradora(administradora);
        usuario.setCondominio(null);

        return salvarComSenhaCifrada(usuario);
    }

    public Usuario cadastrarNoCondominio(Usuario usuario, Condominio condominioJaValidado, Perfil perfilDeQuemCadastra) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADORA) {
            throw new RegraDeNegocioException("Este endpoint não aceita perfil ADMINISTRADORA.");
        }

        if (perfilDeQuemCadastra == Perfil.SINDICO && !PERFIS_QUE_SINDICO_PODE_CRIAR.contains(usuario.getPerfil())) {
            throw new RegraDeNegocioException("Síndico só pode cadastrar moradores e proprietários.");
        }

        validarLoginDisponivel(usuario.getUsuario());

        usuario.setCondominio(condominioJaValidado);
        usuario.setAdministradora(null);

        return salvarComSenhaCifrada(usuario);
    }

    private void validarLoginDisponivel(String login) {
        if (usuarioRepository.existsByUsuario(login)) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este login.");
        }
    }

    private Usuario salvarComSenhaCifrada(Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return usuarioRepository.save(usuario);
    }
}