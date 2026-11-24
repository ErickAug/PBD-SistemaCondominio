package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.SituacaoReuniao;
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
import java.time.LocalTime;

@Entity
@Table(name = "reunioes")
@Getter
@Setter
@NoArgsConstructor
public class Reuniao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "local_reuniao", nullable = false)
    private String local;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoReuniao situacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocada_por_id", nullable = false)
    private Usuario convocadaPor;

    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(name = "ata_texto", columnDefinition = "TEXT")
    private String ataTexto;

    @Column(name = "ata_arquivo", columnDefinition = "TEXT")
    private String ataArquivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ata_anexada_por_id")
    private Usuario ataAnexadaPor;

    @Column(name = "data_anexo_ata")
    private LocalDateTime dataAnexoAta;
}
