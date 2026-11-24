package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
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
@Table(name = "ocorrencias_morador_historico")
@Getter
@Setter
@NoArgsConstructor
public class OcorrenciaMoradorHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocorrencia_id", nullable = false)
    private OcorrenciaMorador ocorrencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_anterior")
    private SituacaoOcorrenciaMorador situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_nova", nullable = false)
    private SituacaoOcorrenciaMorador situacaoNova;

    @Column(length = 4000)
    private String resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}
