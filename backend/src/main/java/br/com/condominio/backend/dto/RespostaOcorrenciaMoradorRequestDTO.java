package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;

public record RespostaOcorrenciaMoradorRequestDTO(SituacaoOcorrenciaMorador situacao, String resposta) {
}
