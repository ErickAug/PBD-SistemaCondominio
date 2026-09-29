package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OcupacaoService {

    private final OcupacaoRepository ocupacaoRepository;
    private final UsuarioRepository usuarioRepository;

    public OcupacaoService(OcupacaoRepository ocupacaoRepository, UsuarioRepository usuarioRepository) {
        this.ocupacaoRepository = ocupacaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Ocupacao registrar(Unidade unidade, TipoOcupacao tipo, Long ocupanteId, LocalDate dataEntrada) {
        Usuario ocupante = resolverOcupante(ocupanteId);

        validarConsistenciaTipoOcupante(unidade, tipo, ocupante);
        validarContinuidadeCronologica(unidade.getId(), dataEntrada);

        Ocupacao ocupacao = new Ocupacao();
        ocupacao.setUnidade(unidade);
        ocupacao.setTipo(tipo);
        ocupacao.setOcupante(ocupante);
        ocupacao.setDataEntrada(dataEntrada);

        return ocupacaoRepository.save(ocupacao);
    }

    public Optional<Ocupacao> obterAtual(Long unidadeId) {
        return ocupacaoRepository.findFirstByUnidadeIdOrderByDataEntradaDesc(unidadeId);
    }

    public Optional<Ocupacao> obterEmData(Long unidadeId, LocalDate data) {
        return ocupacaoRepository.findFirstByUnidadeIdAndDataEntradaLessThanEqualOrderByDataEntradaDesc(unidadeId, data);
    }

    public List<Ocupacao> listarHistorico(Long unidadeId) {
        return ocupacaoRepository.findByUnidadeIdOrderByDataEntradaDesc(unidadeId);
    }

    private Usuario resolverOcupante(Long ocupanteId) {
        if (ocupanteId == null) {
            return null;
        }
        return usuarioRepository.findById(ocupanteId)
                .orElseThrow(() -> new RegraDeNegocioException("Ocupante não encontrado."));
    }

    public Map<Long, Ocupacao> buscarOcupacoesAtuaisEmLote(List<Long> unidadeIds) {
        if (unidadeIds.isEmpty()) {
            return Map.of();
        }
        return ocupacaoRepository.buscarOcupacoesAtuais(unidadeIds).stream()
                .collect(Collectors.toMap(o -> o.getUnidade().getId(), o -> o));
    }

    private void validarConsistenciaTipoOcupante(Unidade unidade, TipoOcupacao tipo, Usuario ocupante) {
        if (tipo == TipoOcupacao.VAZIA) {
            if (ocupante != null) {
                throw new RegraDeNegocioException("Ocupação do tipo VAZIA não pode ter ocupante.");
            }
            return;
        }

        if (ocupante == null) {
            throw new RegraDeNegocioException("Este tipo de ocupação exige um ocupante.");
        }

        if (ocupante.getCondominio() == null || !ocupante.getCondominio().getId().equals(unidade.getBloco().getCondominio().getId())) {
            throw new RegraDeNegocioException("O ocupante não pertence a este condomínio.");
        }

        if (tipo == TipoOcupacao.PROPRIETARIO_MORANDO) {
            boolean ehODono = unidade.getProprietario() != null
                    && unidade.getProprietario().getId().equals(ocupante.getId());

            if (!ehODono) {
                throw new RegraDeNegocioException(
                        "Para este tipo, o ocupante deve ser o proprietário cadastrado desta unidade.");
            }
        }

        if (tipo == TipoOcupacao.ALUGADA && ocupante.getPerfil() != Perfil.MORADOR) {
            throw new RegraDeNegocioException("Para unidade alugada, o ocupante deve ter perfil MORADOR.");
        }
    }

    private void validarContinuidadeCronologica(Long unidadeId, LocalDate dataEntrada) {
        ocupacaoRepository.findFirstByUnidadeIdOrderByDataEntradaDesc(unidadeId)
                .ifPresent(ultimaOcupacao -> {
                    if (!dataEntrada.isAfter(ultimaOcupacao.getDataEntrada())) {
                        throw new RegraDeNegocioException(
                                "A data de entrada deve ser posterior à ocupação mais recente já registrada.");
                    }
                });
    }
}