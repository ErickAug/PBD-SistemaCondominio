package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.Perfil;

public record UsuarioRequestDTO(String nome, String usuario, String senha, Perfil perfil,
                                Long condominioId, Long unidadeId) {
}