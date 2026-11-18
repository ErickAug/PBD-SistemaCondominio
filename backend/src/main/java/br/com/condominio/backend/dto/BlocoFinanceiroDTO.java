package br.com.condominio.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BlocoFinanceiroDTO(
        LocalDate competencia,
        BigDecimal receita,
        List<DespesaPorCategoriaDTO> despesasPorCategoria,
        BigDecimal totalDespesa,
        BigDecimal saldo
) {
}
