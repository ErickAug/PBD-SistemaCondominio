package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.Perfil;

public record LoginResponseDTO(String token, String tipo, Long usuarioId, String nome,
                               Perfil perfil, Long administradoraId, Long condominioId) {
}