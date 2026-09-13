package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AdministradoraRepository administradoraRepository;
    private final CondominioRepository condominioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AdministradoraRepository administradoraRepository,
                          CondominioRepository condominioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.administradoraRepository = administradoraRepository;
        this.condominioRepository = condominioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario cadastrar(Usuario usuario, Long condominioId, Long administradoraIdAutenticado) {
        if (usuarioRepository.existsByUsuario(usuario.getUsuario())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este login.");
        }

        vincularAoTenantCorreto(usuario, condominioId, administradoraIdAutenticado);

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));

        return usuarioRepository.save(usuario);
    }

    private void vincularAoTenantCorreto(Usuario usuario, Long condominioId, Long administradoraIdAutenticado) {
        Administradora administradora = administradoraRepository.findById(administradoraIdAutenticado)
                .orElseThrow(() -> new RegraDeNegocioException("Administradora não encontrada."));

        if (usuario.getPerfil() == Perfil.ADMINISTRADORA) {
            usuario.setAdministradora(administradora);
            usuario.setCondominio(null);
            return;
        }

        if (condominioId == null) {
            throw new RegraDeNegocioException("Este perfil exige um condomínio vinculado.");
        }

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        if (!condominio.getAdministradora().getId().equals(administradoraIdAutenticado)) { // Defesa em Profundidade/ Evita que uma ADM cadastre alguém em um
            throw new RegraDeNegocioException("Acesso negado a este condomínio.");         // condomínio que não a pertence.
        }

        usuario.setCondominio(condominio);
        usuario.setAdministradora(null);
    }
}