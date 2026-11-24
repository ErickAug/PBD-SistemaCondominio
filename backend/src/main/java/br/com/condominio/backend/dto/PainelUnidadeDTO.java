package br.com.condominio.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record PainelUnidadeDTO(MinhaUnidadeDTO unidade, PermissoesPainelDTO permissoes,
                               List<AvisoPainelDTO> avisos, List<CobrancaPainelDTO> cobrancas,
                               SituacaoFinanceiraDTO situacaoFinanceira, List<ReservaResponseDTO> reservas,
                               List<OcorrenciaMoradorResumoDTO> ocorrencias, LocalDate competenciaPrestacaoContas,
                               RelatorioPeriodoDTO prestacaoContas) {
}
