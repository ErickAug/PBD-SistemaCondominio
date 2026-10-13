package br.com.condominio.backend.service;

import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;

@Service
public class MoradiaService {

    private final VinculoMoradorRepository vinculoMoradorRepository;
    private final OcupacaoRepository ocupacaoRepository;

    public MoradiaService(VinculoMoradorRepository vinculoMoradorRepository,
                          OcupacaoRepository ocupacaoRepository) {
        this.vinculoMoradorRepository = vinculoMoradorRepository;
        this.ocupacaoRepository = ocupacaoRepository;
    }

    public boolean moraNaUnidade(Usuario usuario, Long unidadeId) {
        return switch (usuario.getPerfil()) {
            case MORADOR -> vinculoMoradorRepository.findByUnidadeIdAndDataSaidaIsNull(unidadeId)
                    .stream()
                    .anyMatch(v -> v.getMorador().getId().equals(usuario.getId()));

            case PROPRIETARIO -> ocupacaoRepository.findFirstByUnidadeIdOrderByDataEntradaDesc(unidadeId)
                    .filter(o -> o.getTipo() == TipoOcupacao.PROPRIETARIO_MORANDO)
                    .map(o -> o.getOcupante() != null && o.getOcupante().getId().equals(usuario.getId()))
                    .orElse(false);

            default -> false;
        };
    }
}