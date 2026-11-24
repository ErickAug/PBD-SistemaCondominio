package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record LeituraDestinatarioDTO(Long usuarioId, String nome, Long unidadeId, String blocoNome,
                                     String unidadeNumero, String papel, boolean leu,
                                     LocalDateTime dataLeitura) {
}
