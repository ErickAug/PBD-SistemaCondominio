package br.com.condominio.backend.util;

import br.com.condominio.backend.exception.RegraDeNegocioException;

public final class CnpjUtil {

    private CnpjUtil() {
    }

    public static String normalizar(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new RegraDeNegocioException("O CNPJ é obrigatório.");
        }

        String digitos = cnpj.replaceAll("[^0-9]", "");

        if (digitos.length() != 14) {
            throw new RegraDeNegocioException("CNPJ inválido. Deve conter 14 dígitos.");
        }

        return digitos;
    }
}
