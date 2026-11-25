package br.com.condominio.backend.service;

import br.com.condominio.backend.dto.BlocoFinanceiroDTO;
import br.com.condominio.backend.dto.DespesaPorCategoriaDTO;
import br.com.condominio.backend.model.enums.TipoDespesa;
import br.com.condominio.backend.repository.CategoriaTotalProjection;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.DespesaRepository;
import br.com.condominio.backend.repository.ParcelaDespesaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private DespesaRepository despesaRepository;

    @Mock
    private ParcelaDespesaRepository parcelaDespesaRepository;

    @Mock
    private CobrancaRepository cobrancaRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    private final LocalDate competencia = LocalDate.of(2026, 11, 1);
    private final LocalDate inicioMes = LocalDate.of(2026, 11, 1);
    private final LocalDate fimMes = LocalDate.of(2026, 11, 30);
    private final LocalDate inicioMesAnterior = LocalDate.of(2026, 10, 1);
    private final LocalDate fimMesAnterior = LocalDate.of(2026, 10, 31);

    private CategoriaTotalProjection projecao(String categoria, BigDecimal total) {
        CategoriaTotalProjection projecao = Mockito.mock(CategoriaTotalProjection.class);
        when(projecao.getCategoria()).thenReturn(categoria);
        when(projecao.getTotal()).thenReturn(total);
        return projecao;
    }

    @Test
    void inclui_despesas_ordinarias_e_extraordinarias_no_relatorio() {
        CategoriaTotalProjection agua = projecao("Água", new BigDecimal("500.00"));
        CategoriaTotalProjection manutencao = projecao("Manutenção", new BigDecimal("300.00"));
        CategoriaTotalProjection reforma = projecao("Reforma da fachada", new BigDecimal("1000.00"));

        when(despesaRepository.somarPorCategoria(1L, inicioMes, fimMes, TipoDespesa.ORDINARIA))
                .thenReturn(List.of(agua, manutencao));
        when(parcelaDespesaRepository.somarPorCategoria(1L, inicioMes))
                .thenReturn(List.of(reforma));
        when(cobrancaRepository.somarReceitaPorPeriodo(1L, inicioMes, fimMes))
                .thenReturn(new BigDecimal("5000.00"));

        BlocoFinanceiroDTO bloco = relatorioService.calcularBloco(1L, competencia);

        List<DespesaPorCategoriaDTO> ordenado = bloco.despesasPorCategoria().stream()
                .sorted(Comparator.comparing(DespesaPorCategoriaDTO::categoria))
                .toList();

        List<DespesaPorCategoriaDTO> esperado = List.of(
                        new DespesaPorCategoriaDTO("Manutenção", new BigDecimal("300.00")),
                        new DespesaPorCategoriaDTO("Reforma da fachada", new BigDecimal("1000.00")),
                        new DespesaPorCategoriaDTO("Água", new BigDecimal("500.00")))
                .stream()
                .sorted(Comparator.comparing(DespesaPorCategoriaDTO::categoria))
                .toList();

        assertEquals(esperado, ordenado);
        assertEquals(new BigDecimal("1800.00"), bloco.totalDespesa());
        assertEquals(new BigDecimal("3200.00"), bloco.saldo());
    }

    @Test
    void soma_despesa_ordinaria_e_parcela_extraordinaria_da_mesma_categoria() {
        CategoriaTotalProjection manutencaoOrdinaria = projecao("Manutenção", new BigDecimal("200.00"));
        CategoriaTotalProjection manutencaoExtraordinaria = projecao("Manutenção", new BigDecimal("150.00"));

        when(despesaRepository.somarPorCategoria(1L, inicioMes, fimMes, TipoDespesa.ORDINARIA))
                .thenReturn(List.of(manutencaoOrdinaria));
        when(parcelaDespesaRepository.somarPorCategoria(1L, inicioMes))
                .thenReturn(List.of(manutencaoExtraordinaria));
        when(cobrancaRepository.somarReceitaPorPeriodo(1L, inicioMes, fimMes))
                .thenReturn(new BigDecimal("1000.00"));

        BlocoFinanceiroDTO bloco = relatorioService.calcularBloco(1L, competencia);

        assertEquals(1, bloco.despesasPorCategoria().size());
        assertEquals("Manutenção", bloco.despesasPorCategoria().get(0).categoria());
        assertEquals(new BigDecimal("350.00"), bloco.despesasPorCategoria().get(0).valor());
        assertEquals(new BigDecimal("350.00"), bloco.totalDespesa());
    }

    @Test
    void mes_anterior_tambem_soma_despesas_ordinarias_do_proprio_periodo() {
        CategoriaTotalProjection agua = projecao("Água", new BigDecimal("450.00"));

        when(despesaRepository.somarPorCategoria(1L, inicioMesAnterior, fimMesAnterior, TipoDespesa.ORDINARIA))
                .thenReturn(List.of(agua));
        when(parcelaDespesaRepository.somarPorCategoria(1L, inicioMesAnterior))
                .thenReturn(List.of());
        when(cobrancaRepository.somarReceitaPorPeriodo(1L, inicioMesAnterior, fimMesAnterior))
                .thenReturn(new BigDecimal("900.00"));

        BlocoFinanceiroDTO blocoMesAnterior = relatorioService.calcularBloco(1L, competencia.minusMonths(1));

        assertEquals(1, blocoMesAnterior.despesasPorCategoria().size());
        assertEquals("Água", blocoMesAnterior.despesasPorCategoria().get(0).categoria());
        assertEquals(new BigDecimal("450.00"), blocoMesAnterior.totalDespesa());
    }
}
