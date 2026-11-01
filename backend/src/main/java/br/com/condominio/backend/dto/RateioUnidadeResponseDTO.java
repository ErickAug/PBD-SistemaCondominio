package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record RateioUnidadeResponseDTO(Long unidadeId, String unidadeNumero,
                                       BigDecimal valorOrdinario, BigDecimal valorExtraordinario, BigDecimal valorTotal,
                                       String responsavelOrdinarioNome, String responsavelExtraordinarioNome) {}