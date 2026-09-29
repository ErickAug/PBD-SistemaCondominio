package br.com.condominio.backend.security;

import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.service.BlocoService;
import br.com.condominio.backend.service.UnidadeService;
import org.springframework.stereotype.Component;

@Component
public class UnidadeAccessGuard {

    private final TenantAccessGuard tenantAccessGuard;
    private final MoradorUnidadeGuard moradorUnidadeGuard;
    private final BlocoService blocoService;
    private final UnidadeService unidadeService;

    public UnidadeAccessGuard(TenantAccessGuard tenantAccessGuard,
                              MoradorUnidadeGuard moradorUnidadeGuard,
                              BlocoService blocoService,
                              UnidadeService unidadeService) {
        this.tenantAccessGuard = tenantAccessGuard;
        this.moradorUnidadeGuard = moradorUnidadeGuard;
        this.blocoService = blocoService;
        this.unidadeService = unidadeService;
    }

    public Unidade validarAcesso(Long condominioId, Long blocoId, Long unidadeId, UsuarioDetailsImpl usuarioAutenticado) {
        Usuario usuario = usuarioAutenticado.getUsuario();

        if (usuario.getPerfil() == Perfil.MORADOR) {
            moradorUnidadeGuard.validarAcessoDoMorador(unidadeId, usuario);
        } else {
            tenantAccessGuard.validarAcessoGerencialAoCondominio(condominioId, usuarioAutenticado);
        }

        blocoService.buscarValidandoCondominio(blocoId, condominioId);
        return unidadeService.buscarValidandoBloco(unidadeId, blocoId);
    }
}