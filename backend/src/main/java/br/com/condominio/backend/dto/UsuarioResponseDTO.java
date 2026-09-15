package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.Perfil;

public record UsuarioResponseDTO(Long id, String nome, String usuario, Perfil perfil,
                                 Long administradoraId, Long condominioId, Long unidadeId) {
}