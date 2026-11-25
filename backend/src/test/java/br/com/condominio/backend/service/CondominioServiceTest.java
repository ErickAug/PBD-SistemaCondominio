package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.enums.SituacaoCondominio;
import br.com.condominio.backend.repository.AdministradoraRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CondominioServiceTest {

    @Mock
    private CondominioRepository condominioRepository;

    @Mock
    private AdministradoraRepository administradoraRepository;

    @InjectMocks
    private CondominioService condominioService;

    private Administradora administradora() {
        Administradora administradora = new Administradora();
        administradora.setId(1L);
        administradora.setNome("Administradora Teste");
        return administradora;
    }

    private Condominio condominio(String cnpj) {
        Condominio condominio = new Condominio();
        condominio.setNome("Residencial Teste");
        condominio.setEndereco("Rua Exemplo, 123");
        condominio.setCnpj(cnpj);
        condominio.setSituacao(SituacaoCondominio.ATIVO);
        condominio.setDiaVencimento(10);
        return condominio;
    }

    @Test
    void remove_mascara_do_cnpj_antes_de_salvar() {
        when(administradoraRepository.findById(1L)).thenReturn(Optional.of(administradora()));
        lenient().when(condominioRepository.save(any(Condominio.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Condominio salvo = condominioService.cadastrar(condominio("11.111.111/1111-11"), 1L);

        assertEquals("11111111111111", salvo.getCnpj());
    }

    @Test
    void bloqueia_cnpj_com_quantidade_errada_de_digitos() {
        when(administradoraRepository.findById(1L)).thenReturn(Optional.of(administradora()));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> condominioService.cadastrar(condominio("111111111111111111"), 1L));

        assertEquals("CNPJ inválido. Deve conter 14 dígitos.", excecao.getMessage());
    }

    @Test
    void bloqueia_cnpj_em_branco() {
        when(administradoraRepository.findById(1L)).thenReturn(Optional.of(administradora()));

        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> condominioService.cadastrar(condominio("  "), 1L));

        assertEquals("O CNPJ é obrigatório.", excecao.getMessage());
    }
}
