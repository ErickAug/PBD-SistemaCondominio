package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.AlcanceAviso;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AvisoPainelDTO(Long id, String titulo, String texto, AlcanceAviso alcance, LocalDate dataValidade,
                             String autorNome, LocalDateTime dataCriacao, boolean confirmavel, boolean lido,
                             LocalDateTime dataLeitura) {
}
