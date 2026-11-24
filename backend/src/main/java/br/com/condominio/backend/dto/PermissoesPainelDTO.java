package br.com.condominio.backend.dto;

public record PermissoesPainelDTO(boolean autorizarVisita, boolean reservarAreaComum, boolean abrirOcorrencia,
                                  boolean verPrestacaoContas) {
}
