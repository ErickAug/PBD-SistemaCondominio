package br.com.condominio.backend.repository;

import java.math.BigDecimal;

public interface CategoriaTotalProjection {
    String getCategoria();
    BigDecimal getTotal();
}
