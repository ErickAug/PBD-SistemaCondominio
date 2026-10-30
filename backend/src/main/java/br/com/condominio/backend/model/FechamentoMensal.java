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

import java.time.LocalDate;

@Entity
@Table(
        name = "fechamentos_mensais",
        uniqueConstraints = @UniqueConstraint(columnNames = {"condominio_id", "competencia"})
)
@Getter
@Setter
@NoArgsConstructor
public class FechamentoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDate dataFechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechado_por_id", nullable = false)
    private Usuario fechadoPor;
}