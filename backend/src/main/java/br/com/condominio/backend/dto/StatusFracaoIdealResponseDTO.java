package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record StatusFracaoIdealResponseDTO(BigDecimal somaAtual, BigDecimal diferencaParaFechar, boolean fechaEm100) {
}