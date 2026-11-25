package br.com.condominio.backend.util;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CnpjUtilTest {

    @Test
    void remove_pontuacao_mantendo_apenas_os_digitos() {
        assertEquals("11222333000181", CnpjUtil.normalizar("11.222.333/0001-81"));
    }

    @Test
    void aceita_cnpj_ja_sem_mascara() {
        assertEquals("11222333000181", CnpjUtil.normalizar("11222333000181"));
    }

    @Test
    void rejeita_cnpj_com_menos_de_14_digitos() {
        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> CnpjUtil.normalizar("1111111111"));

        assertEquals("CNPJ inválido. Deve conter 14 dígitos.", excecao.getMessage());
    }

    @Test
    void rejeita_cnpj_com_mais_de_14_digitos() {
        RegraDeNegocioException excecao = assertThrows(RegraDeNegocioException.class,
                () -> CnpjUtil.normalizar("111111111111111111"));

        assertEquals("CNPJ inválido. Deve conter 14 dígitos.", excecao.getMessage());
    }

    @Test
    void rejeita_cnpj_nulo_ou_em_branco() {
        RegraDeNegocioException excecaoNulo = assertThrows(RegraDeNegocioException.class,
                () -> CnpjUtil.normalizar(null));
        assertEquals("O CNPJ é obrigatório.", excecaoNulo.getMessage());

        RegraDeNegocioException excecaoBranco = assertThrows(RegraDeNegocioException.class,
                () -> CnpjUtil.normalizar("   "));
        assertEquals("O CNPJ é obrigatório.", excecaoBranco.getMessage());
    }
}
