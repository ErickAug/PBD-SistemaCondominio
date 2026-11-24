package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record ConfirmacaoLeituraResponseDTO(Long avisoId, Long usuarioId, Long unidadeId,
                                            LocalDateTime dataLeitura) {
}
