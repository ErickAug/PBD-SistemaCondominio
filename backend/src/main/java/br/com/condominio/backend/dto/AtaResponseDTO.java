package br.com.condominio.backend.dto;

import java.time.LocalDateTime;

public record AtaResponseDTO(String texto, boolean temArquivo, String arquivo, String anexadaPorNome,
                             LocalDateTime dataAnexo) {
}
