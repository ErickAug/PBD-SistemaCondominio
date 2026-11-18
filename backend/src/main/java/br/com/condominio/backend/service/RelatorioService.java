package br.com.condominio.backend.service;

import br.com.condominio.backend.dto.BlocoFinanceiroDTO;
import br.com.condominio.backend.dto.DespesaPorCategoriaDTO;
import br.com.condominio.backend.dto.RelatorioPeriodoDTO;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.ParcelaDespesaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RelatorioService {

    private final ParcelaDespesaRepository parcelaDespesaRepository;
    private final CobrancaRepository cobrancaRepository;

    public RelatorioService(ParcelaDespesaRepository parcelaDespesaRepository,
                             CobrancaRepository cobrancaRepository) {
        this.parcelaDespesaRepository = parcelaDespesaRepository;
        this.cobrancaRepository = cobrancaRepository;
    }

    public BlocoFinanceiroDTO calcularBloco(Long condominioId, LocalDate competencia) {
        LocalDate inicioMes = competencia.withDayOfMonth(1);
        LocalDate fimMes = inicioMes.plusMonths(1).minusDays(1);

        List<DespesaPorCategoriaDTO> despesasPorCategoria = parcelaDespesaRepository
                .somarPorCategoria(condominioId, inicioMes)
                .stream()
                .map(p -> new DespesaPorCategoriaDTO(p.getCategoria(), p.getTotal()))
                .toList();

        BigDecimal totalDespesa = despesasPorCategoria.stream()
                .map(DespesaPorCategoriaDTO::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal receita = cobrancaRepository.somarReceitaPorPeriodo(condominioId, inicioMes, fimMes);

        BigDecimal saldo = receita.subtract(totalDespesa);

        return new BlocoFinanceiroDTO(inicioMes, receita, despesasPorCategoria, totalDespesa, saldo);
    }

    public BigDecimal calcularInadimplenciaAcumulada(Long condominioId) {
        return cobrancaRepository.somarInadimplenciaAcumulada(condominioId, LocalDate.now());
    }

    public RelatorioPeriodoDTO gerarRelatorio(Long condominioId, LocalDate competencia) {
        BlocoFinanceiroDTO periodo = calcularBloco(condominioId, competencia);
        BlocoFinanceiroDTO mesAnterior = calcularBloco(condominioId, competencia.minusMonths(1));
        BigDecimal inadimplenciaAcumulada = calcularInadimplenciaAcumulada(condominioId);

        return new RelatorioPeriodoDTO(periodo, mesAnterior, inadimplenciaAcumulada);
    }

    public String gerarCsv(RelatorioPeriodoDTO relatorio) {
        StringBuilder csv = new StringBuilder();
        csv.append("Relatorio da prestacao de contas\n");
        csv.append("Competencia,").append(relatorio.periodo().competencia()).append("\n\n");

        adicionarBlocoAoCsv(csv, "Periodo", relatorio.periodo());
        csv.append("\n");
        adicionarBlocoAoCsv(csv, "Mes anterior", relatorio.mesAnterior());
        csv.append("\n");

        csv.append("Inadimplencia acumulada,")
                .append(relatorio.inadimplenciaAcumulada().toPlainString())
                .append("\n");

        return csv.toString();
    }

    private void adicionarBlocoAoCsv(StringBuilder csv, String titulo, BlocoFinanceiroDTO bloco) {
        csv.append(titulo).append(" (").append(bloco.competencia()).append(")\n");
        csv.append("Receita,").append(bloco.receita().toPlainString()).append("\n");
        csv.append("Categoria,Valor\n");
        for (DespesaPorCategoriaDTO item : bloco.despesasPorCategoria()) {
            csv.append(item.categoria()).append(",").append(item.valor().toPlainString()).append("\n");
        }
        csv.append("Total despesas,").append(bloco.totalDespesa().toPlainString()).append("\n");
        csv.append("Saldo,").append(bloco.saldo().toPlainString()).append("\n");
    }
}
