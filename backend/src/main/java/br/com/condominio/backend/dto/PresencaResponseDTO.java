package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record PresencaResponseDTO(Long id, Long reuniaoId, Long unidadeId, String blocoNome, String unidadeNumero,
                                  String compareceu, String registradoPorNome, LocalDateTime dataRegistro) {
}
