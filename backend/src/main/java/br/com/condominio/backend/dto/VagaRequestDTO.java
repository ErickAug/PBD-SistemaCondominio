package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoVaga;

public record VagaRequestDTO(String numero, TipoVaga tipo) {}