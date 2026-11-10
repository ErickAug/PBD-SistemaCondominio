package br.com.condominio.backend.dto;

import br.com.condominio.backend.model.enums.FormaPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BaixaRequestDTO(BigDecimal valorPago, LocalDate dataPagamento, FormaPagamento formaPagamento) {}