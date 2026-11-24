package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;

import java.time.LocalDateTime;

public record HistoricoOcorrenciaMoradorDTO(SituacaoOcorrenciaMorador situacaoAnterior,
                                            SituacaoOcorrenciaMorador situacaoNova, String resposta,
                                            Long responsavelId, String responsavelNome,
                                            LocalDateTime dataHora) {
}
