package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.SituacaoCondominio;

public record CondominioResponseDTO(Long id, String nome, String endereco, String cnpj,
                                    SituacaoCondominio situacao, Long administradoraId) {
}