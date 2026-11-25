package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import br.com.condominio.backend.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OcupacaoRepository ocupacaoRepository;

    @Mock
    private VagaRepository vagaRepository;

    @InjectMocks
    private UnidadeService unidadeService;

    private Bloco blocoA() {
        Bloco bloco = new Bloco();
        bloco.setId(1L);
        bloco.setNome("Bloco A");
        return bloco;
    }

    private Unidade unidade(String numero, Integer andar, BigDecimal area) {
        Unidade unidade = new Unidade();
        unidade.setNumero(numero);
        unidade.setAndar(andar);
        unidade.setArea(area);
        unidade.setFracaoIdeal(new BigDecimal("10.0000"));
        return unidade;
    }

    @Test
    void bloqueia_quando_ja_existe_unidade_com_mesmo_numero_e_andar_no_bloco() {
        Bloco bloco = blocoA();
        Unidade nova = unidade("101", 1, new BigDecimal("50.00"));

        when(unidadeRepository.existsByNumeroAndAndarAndBlocoId("101", 1, 1L)).thenReturn(true);

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> unidadeService.cadastrar(nova, bloco));

        assertEquals("Já existe uma unidade com este número e andar neste bloco.", excecao.getMessage());
    }

    @Test
    void permite_reaproveitar_o_mesmo_numero_em_andar_diferente() {
        Bloco bloco = blocoA();
        Unidade nova = unidade("101", 2, new BigDecimal("50.00"));

        when(unidadeRepository.existsByNumeroAndAndarAndBlocoId("101", 2, 1L)).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        assertDoesNotThrow(() -> unidadeService.cadastrar(nova, bloco));
    }

    @Test
    void permite_cadastrar_unidade_sem_area_informada() {
        Bloco bloco = blocoA();
        Unidade nova = unidade("303", 3, null);

        when(unidadeRepository.existsByNumeroAndAndarAndBlocoId("303", 3, 1L)).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Unidade salva = unidadeService.cadastrar(nova, bloco);

        assertEquals(null, salva.getArea());
    }
}
