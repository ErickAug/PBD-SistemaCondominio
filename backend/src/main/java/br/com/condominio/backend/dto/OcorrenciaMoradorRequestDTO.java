package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;

public record OcorrenciaMoradorRequestDTO(String assunto, TipoOcorrenciaMorador tipo, String descricao, String foto) {
}
