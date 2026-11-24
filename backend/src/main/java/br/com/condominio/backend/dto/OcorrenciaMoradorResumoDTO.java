package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;

import java.time.LocalDateTime;

public record OcorrenciaMoradorResumoDTO(Long id, Long unidadeId, String blocoNome, String unidadeNumero,
                                         String assunto, TipoOcorrenciaMorador tipo,
                                         SituacaoOcorrenciaMorador situacao, boolean temFoto,
                                         String abertoPorNome, LocalDateTime dataAbertura,
                                         String respondidoPorNome, String ultimaResposta,
                                         LocalDateTime dataUltimaResposta) {
}
