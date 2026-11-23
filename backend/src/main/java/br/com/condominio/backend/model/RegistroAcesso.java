package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.OrigemRegistroAcesso;
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
@Table(name = "registros_acesso")
@Getter
@Setter
@NoArgsConstructor
public class RegistroAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String documento;

    @Column(nullable = false)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemRegistroAcesso origem;

    @Column(name = "quem_autorizou")
    private String quemAutorizou;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizacao_visita_id")
    private AutorizacaoVisita autorizacaoVisita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizado_permanente_id")
    private AutorizadoPermanente autorizadoPermanente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porteiro_id", nullable = false)
    private Usuario porteiro;

    @Column(name = "hora_entrada", nullable = false)
    private LocalDateTime horaEntrada;

    @Column(name = "hora_saida")
    private LocalDateTime horaSaida;
}
