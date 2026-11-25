package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.repository.AdministradoraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradoraServiceTest {

    @Mock
    private AdministradoraRepository administradoraRepository;

    @InjectMocks
    private AdministradoraService administradoraService;

    private Administradora administradora(String cnpj) {
        Administradora administradora = new Administradora();
        administradora.setNome("Administradora Teste");
        administradora.setCnpj(cnpj);
        return administradora;
    }

    @Test
    void remove_mascara_do_cnpj_antes_de_salvar() {
        lenient().when(administradoraRepository.save(any(Administradora.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Administradora salva = administradoraService.cadastrar(administradora("11.111.111/1111-11"));

        assertEquals("11111111111111", salva.getCnpj());
    }

    @Test
    void bloqueia_cnpj_com_quantidade_errada_de_digitos() {
        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> administradoraService.cadastrar(administradora("111111111111111111")));

        assertEquals("CNPJ inválido. Deve conter 14 dígitos.", excecao.getMessage());
    }

    @Test
    void bloqueia_cnpj_nulo() {
        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> administradoraService.cadastrar(administradora(null)));

        assertEquals("O CNPJ é obrigatório.", excecao.getMessage());
    }
}
