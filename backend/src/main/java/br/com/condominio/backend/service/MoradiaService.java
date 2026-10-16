package br.com.condominio.backend.service;

import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MoradiaService {

    private final VinculoMoradorRepository vinculoMoradorRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final UnidadeRepository unidadeRepository;

    public MoradiaService(VinculoMoradorRepository vinculoMoradorRepository,
                          OcupacaoRepository ocupacaoRepository,
                          UnidadeRepository unidadeRepository) {
        this.vinculoMoradorRepository = vinculoMoradorRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.unidadeRepository = unidadeRepository;
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

    public Optional<Unidade> unidadeOndeMora(Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case MORADOR -> vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(usuario.getId())
                    .stream()
                    .max(Comparator.comparing(VinculoMorador::getDataEntrada))
                    .map(VinculoMorador::getUnidade);

            case PROPRIETARIO -> {
                List<Long> idsDasUnidades = unidadeRepository.findByProprietarioId(usuario.getId())
                        .stream()
                        .map(Unidade::getId)
                        .toList();

                if (idsDasUnidades.isEmpty()) {
                    yield Optional.empty();
                }

                yield ocupacaoRepository.buscarOcupacoesAtuais(idsDasUnidades)
                        .stream()
                        .filter(o -> o.getTipo() == TipoOcupacao.PROPRIETARIO_MORANDO)
                        .filter(o -> o.getOcupante() != null && o.getOcupante().getId().equals(usuario.getId()))
                        .findFirst()
                        .map(Ocupacao::getUnidade);
            }

            default -> Optional.empty();
        };
    }
}