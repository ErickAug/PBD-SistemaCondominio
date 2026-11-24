package br.com.condominio.backend.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReuniaoRequestDTO(String titulo, LocalDate data, LocalTime hora, String local, List<String> pauta) {
}
