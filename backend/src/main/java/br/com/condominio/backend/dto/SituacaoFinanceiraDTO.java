package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record SituacaoFinanceiraDTO(boolean emDia, int cobrancasEmAtraso, BigDecimal valorEmAtraso) {
}
