package br.com.condominio.backend.dto;

import java.math.BigDecimal;

public record RelatorioPeriodoDTO(
        BlocoFinanceiroDTO periodo,
        BlocoFinanceiroDTO mesAnterior,
        BigDecimal inadimplenciaAcumulada
) {
}
