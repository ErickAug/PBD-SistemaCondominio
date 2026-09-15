package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record UnidadeRequestDTO(String numero, Integer andar, BigDecimal area, BigDecimal fracaoIdeal) {
}