package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Despesa;
import br.com.condominio.backend.model.FechamentoMensal;
import br.com.condominio.backend.model.ParcelaDespesa;
import br.com.condominio.backend.model.enums.TipoDespesa;
import br.com.condominio.backend.repository.DespesaRepository;
import br.com.condominio.backend.repository.ParcelaDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final ParcelaDespesaRepository parcelaDespesaRepository;
    private final FechamentoMensalService fechamentoMensalService;

    public DespesaService(DespesaRepository despesaRepository,
                          ParcelaDespesaRepository parcelaDespesaRepository,
                          FechamentoMensalService fechamentoMensalService) {
        this.despesaRepository = despesaRepository;
        this.parcelaDespesaRepository = parcelaDespesaRepository;
        this.fechamentoMensalService = fechamentoMensalService;
    }

    @Transactional
    public Despesa cadastrar(Despesa despesa, Integer numeroParcelas, Condominio condominio) {
        validarDespesa(despesa);

        despesa.setCondominio(condominio);
        despesa = despesaRepository.save(despesa);

        if (despesa.getTipo() == TipoDespesa.EXTRAORDINARIA) {
            gerarParcelas(despesa, numeroParcelas);
        }

        return despesa;
    }

    @Transactional
    public Despesa atualizar(Long despesaId, Despesa dadosNovos, Integer numeroParcelas, Long condominioId) {
        Despesa despesa = buscarValidandoCondominio(despesaId, condominioId);
        validarDespesa(dadosNovos);

        if (existeCompetenciaFechada(despesa, condominioId)) {
            throw new RegraDeNegocioException("Não é possível alterar: o mês desta despesa já está fechado.");
        }
        if (fechamentoMensalService.estaFechado(condominioId, dadosNovos.getData())) {
            throw new RegraDeNegocioException("Não é possível alterar: o novo mês informado já está fechado.");
        }

        despesa.setData(dadosNovos.getData());
        despesa.setCategoria(dadosNovos.getCategoria());
        despesa.setFornecedor(dadosNovos.getFornecedor());
        despesa.setValor(dadosNovos.getValor());
        despesa.setTipo(dadosNovos.getTipo());
        despesa.setComprovante(dadosNovos.getComprovante());

        parcelaDespesaRepository.deleteByDespesaId(despesa.getId());

        if (despesa.getTipo() == TipoDespesa.EXTRAORDINARIA) {
            gerarParcelas(despesa, numeroParcelas);
        }

        return despesaRepository.save(despesa);
    }

    @Transactional
    public void excluir(Long despesaId, Long condominioId) {
        Despesa despesa = buscarValidandoCondominio(despesaId, condominioId);

        if (existeCompetenciaFechada(despesa, condominioId)) {
            throw new RegraDeNegocioException("Não é possível excluir: o mês desta despesa já está fechado.");
        }

        parcelaDespesaRepository.deleteByDespesaId(despesa.getId());
        despesaRepository.delete(despesa);
    }

    private boolean existeCompetenciaFechada(Despesa despesa, Long condominioId) {
        if (despesa.getTipo() == TipoDespesa.EXTRAORDINARIA) {
            List<ParcelaDespesa> parcelas = parcelaDespesaRepository.findByDespesaIdOrderByNumeroParcelaAsc(despesa.getId());
            for (ParcelaDespesa parcela : parcelas) {
                if (fechamentoMensalService.estaFechado(condominioId, parcela.getCompetencia())) {
                    return true;
                }
            }
            return false;
        }
        return fechamentoMensalService.estaFechado(condominioId, despesa.getData());
    }

    public ResultadoListagem listar(Long condominioId, LocalDate dataInicio, LocalDate dataFim,
                                    String categoria, TipoDespesa tipo) {
        List<Despesa> despesas = despesaRepository.filtrar(condominioId, dataInicio, dataFim, categoria, tipo);
        BigDecimal total = despesaRepository.somarValores(condominioId, dataInicio, dataFim, categoria, tipo);

        return new ResultadoListagem(despesas, total);
    }

    public List<ParcelaDespesa> listarParcelas(Long despesaId) {
        return parcelaDespesaRepository.findByDespesaIdOrderByNumeroParcelaAsc(despesaId);
    }

    public Despesa buscarValidandoCondominio(Long despesaId, Long condominioId) {
        return despesaRepository.findById(despesaId)
                .filter(d -> d.getCondominio().getId().equals(condominioId))
                .orElseThrow(() -> new RegraDeNegocioException("Despesa não encontrada neste condomínio."));
    }

    private void validarDespesa(Despesa despesa) {
        if (despesa.getTipo() == null) {
            throw new RegraDeNegocioException("O tipo da despesa é obrigatório (ORDINARIA ou EXTRAORDINARIA).");
        }
        if (despesa.getData() == null) {
            throw new RegraDeNegocioException("A data da despesa é obrigatória.");
        }
        if (despesa.getCategoria() == null || despesa.getCategoria().isBlank()) {
            throw new RegraDeNegocioException("A categoria da despesa é obrigatória.");
        }
        if (despesa.getFornecedor() == null || despesa.getFornecedor().isBlank()) {
            throw new RegraDeNegocioException("O fornecedor da despesa é obrigatório.");
        }
        if (despesa.getValor() == null || despesa.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor da despesa deve ser maior que zero.");
        }
        if (despesa.getValor() != null) {
            despesa.setValor(despesa.getValor().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void gerarParcelas(Despesa despesa, Integer numeroParcelasInformado) {
        int numeroParcelas = numeroParcelasInformado == null ? 1 : numeroParcelasInformado;

        if (numeroParcelas < 1) {
            throw new RegraDeNegocioException("O número de parcelas deve ser pelo menos 1.");
        }

        BigDecimal valorParcelaBase = despesa.getValor()
                .divide(BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.DOWN);

        BigDecimal somaParcial = valorParcelaBase.multiply(BigDecimal.valueOf(numeroParcelas - 1));
        BigDecimal valorUltimaParcela = despesa.getValor().subtract(somaParcial);

        LocalDate competenciaInicial = despesa.getData().withDayOfMonth(1);
        List<ParcelaDespesa> parcelas = new ArrayList<>();

        for (int numero = 1; numero <= numeroParcelas; numero++) {
            ParcelaDespesa parcela = new ParcelaDespesa();
            parcela.setDespesa(despesa);
            parcela.setNumeroParcela(numero);
            parcela.setCompetencia(competenciaInicial.plusMonths(numero - 1));
            parcela.setValor(numero == numeroParcelas ? valorUltimaParcela : valorParcelaBase);

            parcelas.add(parcela);
        }

        parcelaDespesaRepository.saveAll(parcelas);
    }

    public record ResultadoListagem(List<Despesa> despesas, BigDecimal total) {
    }
}