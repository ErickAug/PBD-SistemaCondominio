package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.AlcanceAviso;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AvisoResponseDTO(Long id, Long condominioId, String titulo, String texto, AlcanceAviso alcance,
                               Long blocoId, String blocoNome, Long unidadeId, String unidadeNumero,
                               LocalDate dataValidade, boolean vencido, String autorNome,
                               LocalDateTime dataCriacao) {
}
