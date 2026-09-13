package br.com.condominio.backend.model;

import br.com.condominio.backend.model.enums.SituacaoCondominio;
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

@Entity
@Table(name = "condominios")
@Getter
@Setter
@NoArgsConstructor
public class Condominio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoCondominio situacao;

    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY para toda vez que eu buscar um condomínio, ele não ir na tabela administradora toda vez.
    @JoinColumn(name = "administradora_id", nullable = false) // Nome da coluna que vai receber
    private Administradora administradora;
}