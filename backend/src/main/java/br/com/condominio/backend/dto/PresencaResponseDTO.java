package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record PresencaResponseDTO(Long id, Long reuniaoId, Long unidadeId, String blocoNome, String unidadeNumero,
                                  Integer unidadeAndar, String compareceu, String registradoPorNome, LocalDateTime dataRegistro) {
}
