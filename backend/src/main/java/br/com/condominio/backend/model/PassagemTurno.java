package br.com.condominio.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "passagens_turno")
@Getter
@Setter
@NoArgsConstructor
public class PassagemTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condominio_id", nullable = false)
    private Condominio condominio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porteiro_id", nullable = false)
    private Usuario porteiro;

    @Column(nullable = false)
    private LocalDateTime horarioEncerramento;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "passagem_turno_pendencias",
            joinColumns = @JoinColumn(name = "passagem_turno_id"),
            inverseJoinColumns = @JoinColumn(name = "ocorrencia_id"))
    private List<Ocorrencia> pendencias = new ArrayList<>();
}
