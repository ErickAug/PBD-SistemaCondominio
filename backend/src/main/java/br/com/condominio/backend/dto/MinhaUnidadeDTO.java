package br.com.condominio.backend.dto;

import java.util.List;

public record MinhaUnidadeDTO(Long unidadeId, Long blocoId, String blocoNome, String unidadeNumero,
                              List<String> papeis) {
}
