package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoVaga;

public record VagaResponseDTO(Long id, String numero, TipoVaga tipo, Long unidadeId, VeiculoResumoDTO veiculo) {}