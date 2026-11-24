package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.AlcanceAviso;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "avisos")
@Getter
@Setter
@NoArgsConstructor
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlcanceAviso alcance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloco_id", nullable = true)
    private Bloco bloco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = true)
    private Unidade unidade;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
}
