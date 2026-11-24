package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "ocorrencias_morador")
@Getter
@Setter
@NoArgsConstructor
public class OcorrenciaMorador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOcorrenciaMorador tipo;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoOcorrenciaMorador situacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aberto_por_id", nullable = false)
    private Usuario abertoPor;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondido_por_id", nullable = true)
    private Usuario respondidoPor;

    @Column(name = "ultima_resposta", length = 4000)
    private String ultimaResposta;

    @Column(name = "data_ultima_resposta")
    private LocalDateTime dataUltimaResposta;
}
