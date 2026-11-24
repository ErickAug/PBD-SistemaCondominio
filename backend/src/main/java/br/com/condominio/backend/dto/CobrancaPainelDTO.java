package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CobrancaPainelDTO(Long cobrancaId, LocalDate competencia, LocalDate vencimento,
                                SituacaoCobranca situacao, boolean emAtraso, Long diasAtraso,
                                BigDecimal parteOrdinaria, BigDecimal parteExtraordinaria, BigDecimal valorTotal,
                                LocalDate dataPagamento) {
}
