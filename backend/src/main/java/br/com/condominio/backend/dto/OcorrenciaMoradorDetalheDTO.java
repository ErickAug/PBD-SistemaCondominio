package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;

import java.time.LocalDateTime;
import java.util.List;

public record OcorrenciaMoradorDetalheDTO(Long id, Long condominioId, Long unidadeId, String blocoNome,
                                          String unidadeNumero, String assunto, TipoOcorrenciaMorador tipo,
                                          String descricao, String foto, SituacaoOcorrenciaMorador situacao,
                                          Long abertoPorId, String abertoPorNome, LocalDateTime dataAbertura,
                                          Long respondidoPorId, String respondidoPorNome, String ultimaResposta,
                                          LocalDateTime dataUltimaResposta,
                                          List<HistoricoOcorrenciaMoradorDTO> historico) {
}
