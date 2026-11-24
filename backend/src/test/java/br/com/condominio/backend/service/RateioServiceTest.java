package br.com.condominio.backend.service;

import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.RateioUnidade;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.ModoRateio;
import br.com.condominio.backend.model.enums.TipoDespesa;
import br.com.condominio.backend.repository.DespesaRepository;
import br.com.condominio.backend.repository.ParcelaDespesaRepository;
import br.com.condominio.backend.repository.RateioMensalRepository;
import br.com.condominio.backend.repository.RateioUnidadeRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateioServiceTest {

    private static final Long CONDOMINIO_ID = 1L;
    private static final LocalDate COMPETENCIA = LocalDate.of(2026, 8, 1);

    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ParcelaDespesaRepository parcelaDespesaRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private OcupacaoService ocupacaoService;
    @Mock
    private UnidadeService unidadeService;
    @Mock
    private RateioMensalRepository rateioMensalRepository;
    @Mock
    private RateioUnidadeRepository rateioUnidadeRepository;
    @Mock
    private CobrancaService cobrancaService;

    @InjectMocks
    private RateioService service;

    private Condominio condominio;
    private Bloco bloco;
    private Usuario dono;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(CONDOMINIO_ID);

        bloco = new Bloco();
        bloco.setId(10L);
        bloco.setCondominio(condominio);

        dono = new Usuario();
        dono.setId(7L);
    }

    @Test
    void igualitarioComDivisaoExataDaOMesmoValorParaTodas() {
        condominio.setModoRateio(ModoRateio.IGUALITARIO);
        prepararUnidades("1", "1", "1");
        prepararTotais("303.00", "153.00");

        List<RateioUnidade> itens = service.calcular(condominio, COMPETENCIA);

        assertEquals(List.of(new BigDecimal("101.00"), new BigDecimal("101.00"), new BigDecimal("101.00")),
                itens.stream().map(RateioUnidade::getValorOrdinario).toList());
        assertEquals(List.of(new BigDecimal("51.00"), new BigDecimal("51.00"), new BigDecimal("51.00")),
                itens.stream().map(RateioUnidade::getValorExtraordinario).toList());
    }

    @Test
    void igualitarioComSobraDeCentavosJogaASobraNaUltimaESomaBate() {
        condominio.setModoRateio(ModoRateio.IGUALITARIO);
        prepararUnidades("1", "1", "1");
        prepararTotais("100.00", "0.00");

        List<RateioUnidade> itens = service.calcular(condominio, COMPETENCIA);

        assertEquals(List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")),
                itens.stream().map(RateioUnidade::getValorOrdinario).toList());
        assertEquals(new BigDecimal("100.00"), soma(itens));
    }

    @Test
    void fracaoIdealComPercentuaisExatosDaValoresExatos() {
        condominio.setModoRateio(ModoRateio.FRACAO_IDEAL);
        prepararUnidades("25.0000", "25.0000", "50.0000");
        prepararTotais("1000.00", "0.00");
        when(unidadeService.calcularStatusFracao(CONDOMINIO_ID))
                .thenReturn(new UnidadeService.StatusFracaoIdeal(new BigDecimal("100"), BigDecimal.ZERO, true));

        List<RateioUnidade> itens = service.calcular(condominio, COMPETENCIA);

        assertEquals(List.of(new BigDecimal("250.00"), new BigDecimal("250.00"), new BigDecimal("500.00")),
                itens.stream().map(RateioUnidade::getValorOrdinario).toList());
    }

    @Test
    void fracaoIdealQuebradaMantemASomaIgualAoTotal() {
        condominio.setModoRateio(ModoRateio.FRACAO_IDEAL);
        prepararUnidades("33.3333", "33.3333", "33.3334");
        prepararTotais("300.00", "0.00");
        when(unidadeService.calcularStatusFracao(CONDOMINIO_ID))
                .thenReturn(new UnidadeService.StatusFracaoIdeal(new BigDecimal("100"), BigDecimal.ZERO, true));

        List<RateioUnidade> itens = service.calcular(condominio, COMPETENCIA);

        assertEquals(List.of(new BigDecimal("99.99"), new BigDecimal("99.99"), new BigDecimal("100.02")),
                itens.stream().map(RateioUnidade::getValorOrdinario).toList());
        assertEquals(new BigDecimal("300.00"), soma(itens));
    }

    private void prepararUnidades(String... fracoes) {
        List<Unidade> unidades = new java.util.ArrayList<>();
        for (int i = 0; i < fracoes.length; i++) {
            Unidade u = new Unidade();
            u.setId(100L + i);
            u.setNumero(String.valueOf(101 + i));
            u.setBloco(bloco);
            u.setFracaoIdeal(new BigDecimal(fracoes[i]));
            u.setProprietario(dono);
            unidades.add(u);
        }
        when(unidadeRepository.findByBlocoCondominioId(CONDOMINIO_ID)).thenReturn(unidades);
    }

    private void prepararTotais(String ordinario, String extraordinario) {
        when(despesaRepository.somarValores(eq(CONDOMINIO_ID), eq(COMPETENCIA), any(), isNull(), eq(TipoDespesa.ORDINARIA)))
                .thenReturn(new BigDecimal(ordinario));
        when(parcelaDespesaRepository.somarValorPorCompetencia(CONDOMINIO_ID, COMPETENCIA))
                .thenReturn(new BigDecimal(extraordinario));
    }

    private BigDecimal soma(List<RateioUnidade> itens) {
        return itens.stream().map(RateioUnidade::getValorOrdinario).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
