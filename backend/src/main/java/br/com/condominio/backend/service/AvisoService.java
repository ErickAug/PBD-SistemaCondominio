package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.AvisoLeitura;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Ocupacao;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.AlcanceAviso;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.TipoOcupacao;
import br.com.condominio.backend.repository.AvisoLeituraRepository;
import br.com.condominio.backend.repository.AvisoRepository;
import br.com.condominio.backend.repository.BlocoRepository;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AvisoService {

    public static final String PAPEL_MORADOR = "MORADOR";
    public static final String PAPEL_PROPRIETARIO = "PROPRIETARIO";
    public static final String PAPEL_ANTIGO_DESTINATARIO = "ANTIGO_DESTINATARIO";

    private final AvisoRepository avisoRepository;
    private final AvisoLeituraRepository avisoLeituraRepository;
    private final BlocoRepository blocoRepository;
    private final UnidadeRepository unidadeRepository;
    private final OcupacaoRepository ocupacaoRepository;
    private final VinculoMoradorRepository vinculoMoradorRepository;

    public AvisoService(AvisoRepository avisoRepository,
                        AvisoLeituraRepository avisoLeituraRepository,
                        BlocoRepository blocoRepository,
                        UnidadeRepository unidadeRepository,
                        OcupacaoRepository ocupacaoRepository,
                        VinculoMoradorRepository vinculoMoradorRepository) {
        this.avisoRepository = avisoRepository;
        this.avisoLeituraRepository = avisoLeituraRepository;
        this.blocoRepository = blocoRepository;
        this.unidadeRepository = unidadeRepository;
        this.ocupacaoRepository = ocupacaoRepository;
        this.vinculoMoradorRepository = vinculoMoradorRepository;
    }

    public Aviso criar(Condominio condominioJaValidado, Usuario autor, String titulo, String texto,
                       AlcanceAviso alcance, Long blocoId, Long unidadeId, LocalDate dataValidade) {
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("Informe o título do aviso.");
        }
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("Informe o texto do aviso.");
        }
        if (alcance == null) {
            throw new RegraDeNegocioException("Informe o alcance do aviso: CONDOMINIO, BLOCO ou UNIDADE.");
        }
        if (dataValidade == null) {
            throw new RegraDeNegocioException("Informe a data de validade do aviso.");
        }
        if (dataValidade.isBefore(LocalDate.now())) {
            throw new RegraDeNegocioException("A validade do aviso não pode ser anterior a hoje.");
        }

        Aviso aviso = new Aviso();
        aviso.setCondominio(condominioJaValidado);
        aviso.setAutor(autor);
        aviso.setTitulo(titulo.trim());
        aviso.setTexto(texto.trim());
        aviso.setAlcance(alcance);
        aviso.setDataValidade(dataValidade);
        aviso.setDataCriacao(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        switch (alcance) {
            case BLOCO -> aviso.setBloco(buscarBlocoDoCondominio(blocoId, condominioJaValidado.getId()));
            case UNIDADE -> {
                Unidade unidade = buscarUnidadeDoCondominio(unidadeId, condominioJaValidado.getId());
                aviso.setUnidade(unidade);
                aviso.setBloco(unidade.getBloco());
            }
            case CONDOMINIO -> {
            }
        }

        return avisoRepository.save(aviso);
    }

    public List<Aviso> listarHistorico(Long condominioId) {
        return avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(condominioId);
    }

    public Aviso buscarNoCondominio(Long condominioId, Long avisoId) {
        return avisoRepository.findByIdAndCondominioId(avisoId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Aviso não encontrado."));
    }

    public boolean estaVencido(Aviso aviso) {
        return LocalDate.now().isAfter(aviso.getDataValidade());
    }

    public List<Destinatario> listarDestinatarios(Aviso aviso) {
        return destinatariosDasUnidades(unidadesDoAlcance(aviso));
    }

    public RelatorioLeitura gerarRelatorioLeitura(Long condominioId, Long avisoId) {
        Aviso aviso = buscarNoCondominio(condominioId, avisoId);

        Map<Long, Destinatario> destinatariosPorUsuario = new LinkedHashMap<>();
        listarDestinatarios(aviso).forEach(d -> destinatariosPorUsuario.putIfAbsent(d.usuario().getId(), d));

        List<SituacaoLeitura> leram = new ArrayList<>();
        Set<Long> quemLeu = new LinkedHashSet<>();

        for (AvisoLeitura leitura : avisoLeituraRepository.findByAvisoId(avisoId)) {
            Long usuarioId = leitura.getUsuario().getId();
            quemLeu.add(usuarioId);

            Destinatario atual = destinatariosPorUsuario.get(usuarioId);
            String papel = atual != null ? atual.papel() : PAPEL_ANTIGO_DESTINATARIO;
            Unidade unidade = atual != null ? atual.unidade() : leitura.getUnidade();

            leram.add(new SituacaoLeitura(leitura.getUsuario(), unidade, papel, leitura.getDataLeitura()));
        }

        List<SituacaoLeitura> naoLeram = destinatariosPorUsuario.values().stream()
                .filter(d -> !quemLeu.contains(d.usuario().getId()))
                .map(d -> new SituacaoLeitura(d.usuario(), d.unidade(), d.papel(), null))
                .toList();

        return new RelatorioLeitura(aviso, leram, naoLeram);
    }

    public List<AvisoDoUsuario> listarAvisosDoUsuario(Long condominioId, Usuario usuario) {
        List<Unidade> unidades = unidadesOndeRecebe(condominioId, usuario);

        if (unidades.isEmpty()) {
            return List.of();
        }

        Set<Long> blocoIds = unidades.stream().map(u -> u.getBloco().getId()).collect(Collectors.toSet());
        Set<Long> unidadeIds = unidades.stream().map(Unidade::getId).collect(Collectors.toSet());

        List<Aviso> avisos = avisoRepository.listarVigentesParaUnidades(
                condominioId, LocalDate.now(), blocoIds, unidadeIds);

        if (avisos.isEmpty()) {
            return List.of();
        }

        Map<Long, AvisoLeitura> leiturasPorAviso = avisoLeituraRepository
                .findByUsuarioIdAndAvisoIdIn(usuario.getId(), avisos.stream().map(Aviso::getId).toList())
                .stream()
                .collect(Collectors.toMap(l -> l.getAviso().getId(), Function.identity(), (a, b) -> a));

        return avisos.stream()
                .map(a -> new AvisoDoUsuario(a, Optional.ofNullable(leiturasPorAviso.get(a.getId()))
                        .map(AvisoLeitura::getDataLeitura)
                        .orElse(null)))
                .toList();
    }

    public AvisoLeitura confirmarLeitura(Long condominioId, Long avisoId, Usuario usuario) {
        Aviso aviso = buscarNoCondominio(condominioId, avisoId);

        if (estaVencido(aviso)) {
            throw new RegraDeNegocioException("Este aviso está vencido e não aceita mais confirmação de leitura.");
        }

        Destinatario destinatario = listarDestinatarios(aviso).stream()
                .filter(d -> d.usuario().getId().equals(usuario.getId()))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Este aviso não é destinado a você."));

        Optional<AvisoLeitura> existente = avisoLeituraRepository.findByAvisoIdAndUsuarioId(avisoId, usuario.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        AvisoLeitura leitura = new AvisoLeitura();
        leitura.setAviso(aviso);
        leitura.setUsuario(usuario);
        leitura.setUnidade(destinatario.unidade());
        leitura.setDataLeitura(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        return avisoLeituraRepository.save(leitura);
    }

    private List<Unidade> unidadesDoAlcance(Aviso aviso) {
        return switch (aviso.getAlcance()) {
            case CONDOMINIO -> unidadeRepository.findByBlocoCondominioId(aviso.getCondominio().getId());
            case BLOCO -> unidadeRepository.findByBlocoId(aviso.getBloco().getId());
            case UNIDADE -> List.of(aviso.getUnidade());
        };
    }

    private List<Unidade> unidadesOndeRecebe(Long condominioId, Usuario usuario) {
        List<Unidade> candidatas;

        if (usuario.getPerfil() == Perfil.MORADOR) {
            candidatas = vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(usuario.getId())
                    .stream().map(VinculoMorador::getUnidade).toList();
        } else if (usuario.getPerfil() == Perfil.PROPRIETARIO) {
            candidatas = unidadeRepository.findByProprietarioId(usuario.getId());
        } else {
            return List.of();
        }

        List<Unidade> doCondominio = candidatas.stream()
                .filter(u -> u.getBloco().getCondominio().getId().equals(condominioId))
                .collect(Collectors.toMap(Unidade::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        return destinatariosDasUnidades(doCondominio).stream()
                .filter(d -> d.usuario().getId().equals(usuario.getId()))
                .map(Destinatario::unidade)
                .toList();
    }

    private List<Destinatario> destinatariosDasUnidades(List<Unidade> unidades) {
        if (unidades.isEmpty()) {
            return List.of();
        }

        List<Long> unidadeIds = unidades.stream().map(Unidade::getId).toList();

        Map<Long, Ocupacao> ocupacaoAtualPorUnidade = ocupacaoRepository.buscarOcupacoesAtuais(unidadeIds)
                .stream()
                .collect(Collectors.toMap(o -> o.getUnidade().getId(), Function.identity(), (a, b) -> a));

        Map<Long, List<VinculoMorador>> vinculosPorUnidade = vinculoMoradorRepository
                .findByUnidadeIdInAndDataSaidaIsNull(unidadeIds)
                .stream()
                .collect(Collectors.groupingBy(v -> v.getUnidade().getId()));

        List<Destinatario> destinatarios = new ArrayList<>();

        for (Unidade unidade : unidades) {
            Map<Long, Usuario> moradores = new LinkedHashMap<>();
            Ocupacao atual = ocupacaoAtualPorUnidade.get(unidade.getId());

            if (atual != null && atual.getTipo() != TipoOcupacao.VAZIA) {
                if (atual.getTipo() == TipoOcupacao.PROPRIETARIO_MORANDO && atual.getOcupante() != null) {
                    moradores.put(atual.getOcupante().getId(), atual.getOcupante());
                }
                vinculosPorUnidade.getOrDefault(unidade.getId(), List.of())
                        .forEach(v -> moradores.putIfAbsent(v.getMorador().getId(), v.getMorador()));
            }

            if (!moradores.isEmpty()) {
                moradores.values().forEach(m -> destinatarios.add(new Destinatario(m, unidade, PAPEL_MORADOR)));
            } else if (unidade.getProprietario() != null) {
                destinatarios.add(new Destinatario(unidade.getProprietario(), unidade, PAPEL_PROPRIETARIO));
            }
        }

        return destinatarios;
    }

    private Bloco buscarBlocoDoCondominio(Long blocoId, Long condominioId) {
        if (blocoId == null) {
            throw new RegraDeNegocioException("Informe o bloco do aviso.");
        }

        Bloco bloco = blocoRepository.findById(blocoId)
                .orElseThrow(() -> new RegraDeNegocioException("Bloco não encontrado."));

        if (!bloco.getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Este bloco não pertence a este condomínio.");
        }

        return bloco;
    }

    private Unidade buscarUnidadeDoCondominio(Long unidadeId, Long condominioId) {
        if (unidadeId == null) {
            throw new RegraDeNegocioException("Informe a unidade do aviso.");
        }

        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

        if (!unidade.getBloco().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Esta unidade não pertence a este condomínio.");
        }

        return unidade;
    }

    public record Destinatario(Usuario usuario, Unidade unidade, String papel) {
    }

    public record SituacaoLeitura(Usuario usuario, Unidade unidade, String papel, LocalDateTime dataLeitura) {
    }

    public record RelatorioLeitura(Aviso aviso, List<SituacaoLeitura> leram, List<SituacaoLeitura> naoLeram) {
    }

    public record AvisoDoUsuario(Aviso aviso, LocalDateTime dataLeitura) {
    }
}
