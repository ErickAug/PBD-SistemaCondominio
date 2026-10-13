package br.com.condominio.backend.dto;

import java.util.List;

public record VagasDaUnidadeResponseDTO(long quantidade, List<VagaResponseDTO> vagas) {}
