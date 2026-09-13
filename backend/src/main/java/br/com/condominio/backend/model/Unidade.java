package br.com.condominio.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "unidades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"numero", "bloco_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private Integer andar;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area; // BigDecimal para evitar erros de arredondamentos

    @Column(name = "fracao_ideal", nullable = false, precision = 7, scale = 4)
    private BigDecimal fracaoIdeal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloco_id", nullable = false)
    private Bloco bloco;
}