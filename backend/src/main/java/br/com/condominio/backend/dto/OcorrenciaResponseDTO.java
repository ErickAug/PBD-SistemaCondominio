package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record OcorrenciaResponseDTO(Long id, Long unidadeId, String blocoNome, String unidadeNumero,
                                     String texto, String tipo, String autorNome, LocalDateTime dataHora,
                                     Long correcaoDeId, boolean resolvida, String resolvidoPorNome,
                                     LocalDateTime resolvidoEm) {
}
