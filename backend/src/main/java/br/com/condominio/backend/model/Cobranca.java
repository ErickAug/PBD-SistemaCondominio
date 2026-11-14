package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.FormaPagamento;
import br.com.condominio.backend.model.enums.SituacaoCobranca;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cobrancas")
@Getter
@Setter
@NoArgsConstructor
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rateio_unidade_id", nullable = false, unique = true)
    private RateioUnidade rateioUnidade;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoCobranca situacao = SituacaoCobranca.ABERTA;

    @Column(name = "valor_pago", precision = 10, scale = 2, nullable = true)
    private BigDecimal valorPago;

    @Column(name = "data_pagamento", nullable = true)
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = true)
    private FormaPagamento formaPagamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baixado_por_id", nullable = true)
    private Usuario baixadoPor;

    public BigDecimal getValorCobrado() {
        return rateioUnidade.getValorTotal();
    }

    public BigDecimal getDiferenca() {
        return valorPago != null ? valorPago.subtract(getValorCobrado()) : null;
    }

    public boolean isEmAtraso() {
        return situacao == SituacaoCobranca.ABERTA && vencimento.isBefore(LocalDate.now());
    }
}