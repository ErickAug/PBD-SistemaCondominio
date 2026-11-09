package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.*;
import br.com.condominio.backend.model.enums.ModoRateio;
import br.com.condominio.backend.model.enums.TipoDespesa;
import br.com.condominio.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RateioService {

    private static final BigDecimal CEM_POR_CENTO = new BigDecimal("100");

    private final DespesaRepository despesaRepository;
    private final ParcelaDespesaRepository parcelaDespesaRepository;
    private final UnidadeRepository unidadeRepository;
    private final OcupacaoService ocupacaoService;
    private final UnidadeService unidadeService;
    private final RateioMensalRepository rateioMensalRepository;
    private final RateioUnidadeRepository rateioUnidadeRepository;

    public RateioService(DespesaRepository despesaRepository, ParcelaDespesaRepository parcelaDespesaRepository,
                         UnidadeRepository unidadeRepository, OcupacaoService ocupacaoService,
                         UnidadeService unidadeService, RateioMensalRepository rateioMensalRepository,
                         RateioUnidadeRepository rateioUnidadeRepository) {
        this.despesaRepository = despesaRepository;
        this.parcelaDespesaRepository = parcelaDespesaRepository;
        this.unidadeRepository = unidadeRepository;
        this.ocupacaoService = ocupacaoService;
        this.unidadeService = unidadeService;
        this.rateioMensalRepository = rateioMensalRepository;
        this.rateioUnidadeRepository = rateioUnidadeRepository;
    }

    public List<RateioUnidade> calcular(Condominio condominio, LocalDate competenciaInformada) {
        LocalDate competencia = competenciaInformada.withDayOfMonth(1);

        if (rateioMensalRepository.existsByCondominioIdAndCompetencia(condominio.getId(), competencia)) {
            throw new RegraDeNegocioException("O rateio desta competência já foi fechado.");
        }

        List<Unidade> unidades = unidadeRepository.findByBlocoCondominioId(condominio.getId());
        if (unidades.isEmpty()) {
            throw new RegraDeNegocioException("Este condomínio não possui unidades cadastradas.");
        }

        if (condominio.getModoRateio() == ModoRateio.FRACAO_IDEAL) {
            UnidadeService.StatusFracaoIdeal status = unidadeService.calcularStatusFracao(condominio.getId());
            if (!status.fechaEm100()) {
                throw new RegraDeNegocioException(
                        "A soma das frações ideais das unidades não fecha em 100%% (diferença: "
                                + status.diferencaParaFechar() + "). Ajuste antes de ratear.");
            }
        }

        BigDecimal totalOrdinario = despesaRepository.somarValores(
                condominio.getId(), competencia, competencia.plusMonths(1).minusDays(1), null, TipoDespesa.ORDINARIA);
        BigDecimal totalExtraordinario = parcelaDespesaRepository.somarValorPorCompetencia(condominio.getId(), competencia);

        List<RateioUnidade> itens = distribuir(unidades, condominio, competencia, totalOrdinario, totalExtraordinario);

        List<String> unidadesComProblema = itens.stream()
                .filter(item -> item.getResponsavelOrdinario() == null || item.getResponsavelExtraordinario() == null)
                .map(item -> item.getUnidade().getNumero())
                .toList();

        if (!unidadesComProblema.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Não é possível ratear: as unidades " + unidadesComProblema +
                            " não têm proprietário e/ou ocupante cadastrado para esta competência.");
        }

        return itens;
    }

    @Transactional
    public RateioMensal fechar(Condominio condominio, LocalDate competenciaInformada, FechamentoMensal fechamento) {
        LocalDate competencia = competenciaInformada.withDayOfMonth(1);
        List<RateioUnidade> itens = calcular(condominio, competenciaInformada);

        BigDecimal totalOrdinario = itens.stream().map(RateioUnidade::getValorOrdinario).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExtraordinario = itens.stream().map(RateioUnidade::getValorExtraordinario).reduce(BigDecimal.ZERO, BigDecimal::add);

        RateioMensal rateio = new RateioMensal();
        rateio.setCondominio(condominio);
        rateio.setCompetencia(competencia);
        rateio.setValorTotalOrdinario(totalOrdinario);
        rateio.setValorTotalExtraordinario(totalExtraordinario);
        rateio.setFechamento(fechamento);
        rateio = rateioMensalRepository.save(rateio);

        for (RateioUnidade item : itens) {
            item.setRateioMensal(rateio);
        }
        rateioUnidadeRepository.saveAll(itens);

        return rateio;
    }

    public List<RateioUnidade> listarItens(Long rateioMensalId) {
        return rateioUnidadeRepository.findByRateioMensalIdOrderByUnidadeNumeroAsc(rateioMensalId);
    }

    private List<RateioUnidade> distribuir(List<Unidade> unidades, Condominio condominio, LocalDate competencia,
                                           BigDecimal totalOrdinario, BigDecimal totalExtraordinario) {
        List<RateioUnidade> itens = new ArrayList<>();
        BigDecimal somaOrdinario = BigDecimal.ZERO;
        BigDecimal somaExtraordinario = BigDecimal.ZERO;

        for (int i = 0; i < unidades.size(); i++) {
            Unidade unidade = unidades.get(i);
            boolean ultima = i == unidades.size() - 1;

            BigDecimal fator = condominio.getModoRateio() == ModoRateio.IGUALITARIO
                    ? BigDecimal.ONE.divide(BigDecimal.valueOf(unidades.size()), 10, RoundingMode.HALF_UP)
                    : unidade.getFracaoIdeal().divide(CEM_POR_CENTO, 10, RoundingMode.HALF_UP);

            BigDecimal valorOrdinario = ultima
                    ? totalOrdinario.subtract(somaOrdinario)
                    : totalOrdinario.multiply(fator).setScale(2, RoundingMode.DOWN);
            BigDecimal valorExtraordinario = ultima
                    ? totalExtraordinario.subtract(somaExtraordinario)
                    : totalExtraordinario.multiply(fator).setScale(2, RoundingMode.DOWN);

            somaOrdinario = somaOrdinario.add(valorOrdinario);
            somaExtraordinario = somaExtraordinario.add(valorExtraordinario);

            RateioUnidade item = new RateioUnidade();
            item.setUnidade(unidade);
            item.setValorOrdinario(valorOrdinario);
            item.setValorExtraordinario(valorExtraordinario);
            item.setResponsavelOrdinario(resolverResponsavelOrdinario(unidade, competencia));
            item.setResponsavelExtraordinario(unidade.getProprietario());

            itens.add(item);
        }

        return itens;
    }

    private Usuario resolverResponsavelOrdinario(Unidade unidade, LocalDate competencia) {
        return ocupacaoService.obterEmData(unidade.getId(), competencia)
                .map(Ocupacao::getOcupante)
                .orElse(unidade.getProprietario());
    }
}