package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.ModoRateio;
import br.com.condominio.backend.model.enums.SituacaoCondominio;

public record CondominioRequestDTO(String nome, String endereco, String cnpj,
                                   SituacaoCondominio situacao, ModoRateio modoRateio) {}