package br.com.condominio.backend.security;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.CondominioRepository;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessGuard {

    private final CondominioRepository condominioRepository;

    public TenantAccessGuard(CondominioRepository condominioRepository) {
        this.condominioRepository = condominioRepository;
    }

    public Long validarAdministradora(Long administradoraIdDaUrl, UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdDoToken = usuarioAutenticado.getUsuario().getAdministradora() != null
                ? usuarioAutenticado.getUsuario().getAdministradora().getId()
                : null;

        if (administradoraIdDoToken == null || !administradoraIdDoToken.equals(administradoraIdDaUrl)) {
            throw new RegraDeNegocioException("Acesso negado.");
        }

        return administradoraIdDoToken;
    }

    public Condominio validarAcessoGerencialAoCondominio(Long condominioId, UsuarioDetailsImpl usuarioAutenticado) {
        Usuario usuario = usuarioAutenticado.getUsuario();

        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        boolean administradoraDona = usuario.getPerfil() == Perfil.ADMINISTRADORA
                && usuario.getAdministradora() != null
                && usuario.getAdministradora().getId().equals(condominio.getAdministradora().getId());

        boolean sindicoDoCondominio = usuario.getPerfil() == Perfil.SINDICO
                && usuario.getCondominio() != null
                && usuario.getCondominio().getId().equals(condominioId);

        if (!administradoraDona && !sindicoDoCondominio) {
            throw new RegraDeNegocioException("Acesso negado a este condomínio.");
        }

        return condominio;
    }
}