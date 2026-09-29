package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.TipoOcupacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "ocupacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"unidade_id", "data_entrada"})
)
@Getter
@Setter
@NoArgsConstructor
public class Ocupacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOcupacao tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocupante_id", nullable = true)
    private Usuario ocupante;

    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;
}