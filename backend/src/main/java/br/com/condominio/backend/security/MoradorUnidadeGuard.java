package br.com.condominio.backend.security;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.service.MoradiaService;
import org.springframework.stereotype.Component;

@Component
public class MoradorUnidadeGuard {

    private final MoradiaService moradiaService;

    public MoradorUnidadeGuard(MoradiaService moradiaService) {
        this.moradiaService = moradiaService;
    }

    public void validarAcessoDoMorador(Long unidadeId, Usuario usuario) {
        if (usuario.getPerfil() != Perfil.MORADOR || !moradiaService.moraNaUnidade(usuario, unidadeId)) {
            throw new RegraDeNegocioException("Acesso negado a esta unidade.");
        }
    }

    public void validarAcessoDoProprietarioMorando(Long unidadeId, Usuario usuario) {
        if (usuario.getPerfil() != Perfil.PROPRIETARIO || !moradiaService.moraNaUnidade(usuario, unidadeId)) {
            throw new RegraDeNegocioException("Acesso negado a esta unidade.");
        }
    }
}