package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.RateioMensal;
import br.com.condominio.backend.model.RateioUnidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.FormaPagamento;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import br.com.condominio.backend.repository.CobrancaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;

    public CobrancaService(CobrancaRepository cobrancaRepository) {
        this.cobrancaRepository = cobrancaRepository;
    }

    public List<Cobranca> gerar(RateioMensal rateio, List<RateioUnidade> itens, Integer diaVencimento) {
        LocalDate vencimento = rateio.getCompetencia().plusMonths(1).withDayOfMonth(diaVencimento);

        List<Cobranca> cobrancas = new ArrayList<>();
        for (RateioUnidade item : itens) {
            if (cobrancaRepository.findByRateioUnidadeId(item.getId()).isPresent()) {
                continue;
            }
            Cobranca cobranca = new Cobranca();
            cobranca.setRateioUnidade(item);
            cobranca.setVencimento(vencimento);
            cobrancas.add(cobranca);
        }

        return cobrancaRepository.saveAll(cobrancas);
    }
    public Cobranca buscar(Long cobrancaId) {
        return cobrancaRepository.findById(cobrancaId)
                .orElseThrow(() -> new RegraDeNegocioException("Cobrança não encontrada."));
    }

    public Cobranca buscarValidandoCondominio(Long cobrancaId, Long condominioId) {
        Cobranca cobranca = buscar(cobrancaId);

        if (!cobranca.getRateioUnidade().getRateioMensal().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Cobrança não encontrada neste condomínio.");
        }

        return cobranca;
    }

    @Transactional
    public Cobranca darBaixa(Long cobrancaId, Long condominioId, BigDecimal valorPago, LocalDate dataPagamento,
                             FormaPagamento formaPagamento, Usuario usuarioQueDeuBaixa) {
        Cobranca cobranca = buscarValidandoCondominio(cobrancaId, condominioId);

        if (cobranca.getSituacao() == SituacaoCobranca.PAGA) {
            throw new RegraDeNegocioException("Esta cobrança já está paga.");
        }
        if (valorPago == null || valorPago.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("O valor pago deve ser maior que zero.");
        }
        if (dataPagamento == null) {
            throw new RegraDeNegocioException("A data do pagamento é obrigatória.");
        }
        if (formaPagamento == null) {
            throw new RegraDeNegocioException("A forma de pagamento é obrigatória.");
        }

        cobranca.setValorPago(valorPago.setScale(2, RoundingMode.HALF_UP));
        cobranca.setDataPagamento(dataPagamento);
        cobranca.setFormaPagamento(formaPagamento);
        cobranca.setBaixadoPor(usuarioQueDeuBaixa);
        cobranca.setSituacao(SituacaoCobranca.PAGA);

        return cobrancaRepository.save(cobranca);
    }

    @Transactional
    public void excluir(Long cobrancaId, Long condominioId) {
        Cobranca cobranca = buscarValidandoCondominio(cobrancaId, condominioId);

        if (cobranca.getSituacao() == SituacaoCobranca.PAGA) {
            throw new RegraDeNegocioException("Não é possível excluir: esta cobrança já está paga.");
        }

        cobrancaRepository.delete(cobranca);
    }
    public List<Cobranca> listar(Long condominioId, LocalDate competencia, SituacaoCobranca situacao) {
        return cobrancaRepository.filtrar(condominioId, competencia, situacao);
    }

    public List<Cobranca> listarInadimplencia(Long condominioId) {
        return cobrancaRepository.listarInadimplentes(condominioId, LocalDate.now());
    }

    public List<Cobranca> listarPorUnidade(Long unidadeId) {
        return cobrancaRepository.findByUnidadeId(unidadeId);
    }
}