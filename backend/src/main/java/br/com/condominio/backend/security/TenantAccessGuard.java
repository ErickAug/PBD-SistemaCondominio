package br.com.condominio.backend.security;

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
}