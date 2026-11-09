package br.com.condominio.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "rateios_mensais",
        uniqueConstraints = @UniqueConstraint(columnNames = {"condominio_id", "competencia"})
)
@Getter
@Setter
@NoArgsConstructor
public class RateioMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(name = "valor_total_ordinario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotalOrdinario;

    @Column(name = "valor_total_extraordinario", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotalExtraordinario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechamento_mensal_id", nullable = false, unique = true)
    private FechamentoMensal fechamento;
}