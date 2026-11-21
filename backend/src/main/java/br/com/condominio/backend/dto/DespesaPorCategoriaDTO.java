package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record DespesaPorCategoriaDTO(String categoria, BigDecimal valor) {
}
