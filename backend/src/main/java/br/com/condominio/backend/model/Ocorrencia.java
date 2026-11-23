package br.com.condominio.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ocorrencias")
@Getter
@Setter
@NoArgsConstructor
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(nullable = false)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correcao_de_id")
    private Ocorrencia correcaoDe;

    @Column(nullable = false)
    private boolean resolvida = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolvido_por_id")
    private Usuario resolvidoPor;

    private LocalDateTime resolvidoEm;
}
