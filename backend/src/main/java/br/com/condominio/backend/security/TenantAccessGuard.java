package br.com.condominio.backend.security;

import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import org.springframework.stereotype.Component;

@Component
public class TenantAccessGuard {

    public Long validarAdministradora(Long administradoraIdDaUrl, UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdDoToken = usuarioAutenticado.getUsuario().getAdministradora() != null
                ? usuarioAutenticado.getUsuario().getAdministradora().getId()
                : null;

        if (administradoraIdDoToken == null || !administradoraIdDoToken.equals(administradoraIdDaUrl)) {
            throw new RegraDeNegocioException("Acesso negado.");
        }

        return administradoraIdDoToken;
    }


public Long validarSindicoDoCondominio(Long condominioIdDaUrl, UsuarioDetailsImpl usuarioAutenticado) {
    Usuario usuario = usuarioAutenticado.getUsuario();

    boolean ehSindico = usuario.getPerfil() == Perfil.SINDICO;
    Long condominioIdDoToken = usuario.getCondominio() != null
            ? usuario.getCondominio().getId()
            : null;

    if (!ehSindico || condominioIdDoToken == null || !condominioIdDoToken.equals(condominioIdDaUrl)) {
        throw new RegraDeNegocioException("Acesso negado.");
    }

    return condominioIdDoToken;
    }
}