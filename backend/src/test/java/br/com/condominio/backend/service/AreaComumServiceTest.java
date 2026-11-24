package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AreaComum;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.repository.AreaComumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AreaComumServiceTest {

    @Mock
    private AreaComumRepository areaComumRepository;

    @InjectMocks
    private AreaComumService service;

    private Condominio condominio;

    @BeforeEach
    void setUp() {
        condominio = new Condominio();
        condominio.setId(1L);
        lenient().when(areaComumRepository.save(any(AreaComum.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cadastraAreaComNomeCapacidadeHorarioEAprovacao() {
        AreaComum area = service.cadastrar(condominio, " Salao de festas ", 80, LocalTime.of(8, 0),
                LocalTime.of(22, 0), true);

        assertEquals("Salao de festas", area.getNome());
        assertEquals(80, area.getCapacidade());
        assertEquals(LocalTime.of(8, 0), area.getHorarioAbertura());
        assertEquals(LocalTime.of(22, 0), area.getHorarioFechamento());
        assertTrue(area.isExigeAprovacao());
        assertSame(condominio, area.getCondominio());
    }

    @Test
    void semNomeEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, " ", 10, LocalTime.of(8, 0), LocalTime.of(22, 0), false));

        assertEquals("Informe o nome da área comum.", erro.getMessage());
    }

    @Test
    void capacidadeZeroEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, "Churrasqueira", 0, LocalTime.of(8, 0), LocalTime.of(22, 0), false));

        assertEquals("A capacidade deve ser maior que zero.", erro.getMessage());
    }

    @Test
    void semHorarioEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, "Churrasqueira", 10, null, LocalTime.of(22, 0), false));

        assertEquals("Informe o horário de abertura e de fechamento da área comum.", erro.getMessage());
    }

    @Test
    void aberturaDepoisDoFechamentoEhRecusada() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, "Churrasqueira", 10, LocalTime.of(22, 0), LocalTime.of(8, 0), false));

        assertEquals("O horário de abertura deve ser anterior ao horário de fechamento.", erro.getMessage());
    }

    @Test
    void semInformarSeExigeAprovacaoEhRecusado() {
        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, "Churrasqueira", 10, LocalTime.of(8, 0), LocalTime.of(22, 0), null));

        assertEquals("Informe se a área comum exige aprovação do síndico.", erro.getMessage());
    }

    @Test
    void nomeRepetidoNoCondominioEhRecusado() {
        when(areaComumRepository.existsByCondominioIdAndNomeIgnoreCase(1L, "Piscina")).thenReturn(true);

        RegraDeNegocioException erro = assertThrows(RegraDeNegocioException.class,
                () -> service.cadastrar(condominio, "Piscina", 30, LocalTime.of(8, 0), LocalTime.of(20, 0), false));

        assertEquals("Já existe uma área comum com este nome neste condomínio.", erro.getMessage());
        verify(areaComumRepository, never()).save(any());
    }
}
