package br.com.condominio.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "rateio_unidades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rateio_mensal_id", "unidade_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class RateioUnidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rateio_mensal_id", nullable = false)
    private RateioMensal rateioMensal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(name = "valor_ordinario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorOrdinario;

    @Column(name = "valor_extraordinario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorExtraordinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_ordinario_id", nullable = true)
    private Usuario responsavelOrdinario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_extraordinario_id", nullable = true)
    private Usuario responsavelExtraordinario;

    public BigDecimal getValorTotal() {
        return valorOrdinario.add(valorExtraordinario);
    }
}