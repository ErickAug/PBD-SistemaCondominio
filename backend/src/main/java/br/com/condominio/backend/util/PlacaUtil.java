package br.com.condominio.backend.util;

import br.com.condominio.backend.exception.RegraDeNegocioException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PlacaUtil {

    private static final Pattern FORMATO = Pattern.compile("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}");

    private PlacaUtil() {
    }

    public static String normalizar(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new RegraDeNegocioException("A placa é obrigatória.");
        }

        String limpa = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);

        if (!FORMATO.matcher(limpa).matches()) {
            throw new RegraDeNegocioException("Placa inválida. Use o formato ABC-1234 ou ABC1D23.");
        }

        return limpa;
    }
}