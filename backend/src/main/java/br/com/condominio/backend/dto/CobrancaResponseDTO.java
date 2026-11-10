package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.FormaPagamento;
import br.com.condominio.backend.model.enums.SituacaoCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CobrancaResponseDTO(
        Long id, Long unidadeId, String unidadeNumero, LocalDate competencia, LocalDate vencimento,
        SituacaoCobranca situacao, boolean emAtraso, Long diasAtraso,
        BigDecimal valorOrdinario, BigDecimal valorExtraordinario, BigDecimal valorTotal,
        String responsavelOrdinarioNome, String responsavelExtraordinarioNome,
        BigDecimal valorPago, LocalDate dataPagamento, FormaPagamento formaPagamento,
        BigDecimal diferenca, String baixadoPorNome
) {}