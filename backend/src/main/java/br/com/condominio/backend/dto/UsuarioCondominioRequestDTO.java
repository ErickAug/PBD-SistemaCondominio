package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.Perfil;

public record UsuarioCondominioRequestDTO(String nome, String usuario, String senha, Perfil perfil, String contato) {}