package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradoraRepository administradoraRepository;
    private final CondominioRepository condominioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AdministradoraRepository administradoraRepository,
                          CondominioRepository condominioRepository,
                          UnidadeRepository unidadeRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.administradoraRepository = administradoraRepository;
        this.condominioRepository = condominioRepository;
        this.unidadeRepository = unidadeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(Usuario usuario, Long condominioId, Long unidadeId, Long administradoraIdAutenticado) {
        if (usuarioRepository.existsByUsuario(usuario.getUsuario())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este login.");
        }

        vincularAoTenantCorreto(usuario, condominioId, unidadeId, administradoraIdAutenticado);

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        return usuarioRepository.save(usuario);
    }

    private void vincularAoTenantCorreto(Usuario usuario, Long condominioId, Long unidadeId,
                                         Long administradoraIdAutenticado) {
        Administradora administradora = administradoraRepository.findById(administradoraIdAutenticado)
                .orElseThrow(() -> new RegraDeNegocioException("Administradora não encontrada."));

        if (usuario.getPerfil() == Perfil.ADMINISTRADORA) {
            if (unidadeId != null) {
                throw new RegraDeNegocioException("Este perfil não aceita vínculo com unidade.");
            }
            usuario.setAdministradora(administradora);
            usuario.setCondominio(null);
            usuario.setUnidade(null);
            return;
        }

        if (condominioId == null) {
            throw new RegraDeNegocioException("Este perfil exige um condomínio vinculado.");
        }

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        if (!condominio.getAdministradora().getId().equals(administradoraIdAutenticado)) {
            throw new RegraDeNegocioException("Acesso negado a este condomínio.");
        }

        usuario.setCondominio(condominio);
        usuario.setAdministradora(null);

        boolean perfilAceitaUnidade = usuario.getPerfil() == Perfil.MORADOR
                || usuario.getPerfil() == Perfil.PROPRIETARIO;

        if (unidadeId != null) {
            if (!perfilAceitaUnidade) {
                throw new RegraDeNegocioException("Este perfil não aceita vínculo com unidade.");
            }

            Unidade unidade = unidadeRepository.findById(unidadeId)
                    .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

            if (!unidade.getBloco().getCondominio().getId().equals(condominioId)) {
                throw new RegraDeNegocioException("Esta unidade não pertence ao condomínio informado.");
            }

            usuario.setUnidade(unidade);
        } else {
            usuario.setUnidade(null);
        }
    }
}