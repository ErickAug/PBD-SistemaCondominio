package br.com.condominio.backend.security;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Component;

@Component
public class MoradorUnidadeGuard {

    private final VinculoMoradorRepository vinculoMoradorRepository;

    public MoradorUnidadeGuard(VinculoMoradorRepository vinculoMoradorRepository) {
        this.vinculoMoradorRepository = vinculoMoradorRepository;
    }

    public void validarAcessoDoMorador(Long unidadeId, Usuario usuario) {
        if (usuario.getPerfil() != Perfil.MORADOR) {
            throw new RegraDeNegocioException("Acesso negado.");
        }

        boolean vinculadoAtivo = vinculoMoradorRepository.findByUnidadeIdAndDataSaidaIsNull(unidadeId)
                .stream()
                .anyMatch(v -> v.getMorador().getId().equals(usuario.getId()));

        if (!vinculadoAtivo) {
            throw new RegraDeNegocioException("Acesso negado a esta unidade.");
        }
    }
}