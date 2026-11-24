package br.com.condominio.backend.service;

import br.com.condominio.backend.dto.RelatorioPeriodoDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Aviso;
import br.com.condominio.backend.model.Cobranca;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.RateioUnidade;
import br.com.condominio.backend.model.Reserva;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.AlcanceAviso;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.AvisoRepository;
import br.com.condominio.backend.repository.CobrancaRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MinhaAreaService {

    public static final String PAPEL_MORADOR = "MORADOR";
    public static final String PAPEL_PROPRIETARIO = "PROPRIETARIO";

    private final UnidadeRepository unidadeRepository;
    private final VinculoMoradorRepository vinculoMoradorRepository;
    private final MoradiaService moradiaService;
    private final AvisoService avisoService;
    private final AvisoRepository avisoRepository;
    private final CobrancaRepository cobrancaRepository;
    private final ReservaService reservaService;
    private final OcorrenciaMoradorService ocorrenciaMoradorService;
    private final RelatorioService relatorioService;

    public MinhaAreaService(UnidadeRepository unidadeRepository,
                            VinculoMoradorRepository vinculoMoradorRepository,
                            MoradiaService moradiaService,
                            AvisoService avisoService,
                            AvisoRepository avisoRepository,
                            CobrancaRepository cobrancaRepository,
                            ReservaService reservaService,
                            OcorrenciaMoradorService ocorrenciaMoradorService,
                            RelatorioService relatorioService) {
        this.unidadeRepository = unidadeRepository;
        this.vinculoMoradorRepository = vinculoMoradorRepository;
        this.moradiaService = moradiaService;
        this.avisoService = avisoService;
        this.avisoRepository = avisoRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.reservaService = reservaService;
        this.ocorrenciaMoradorService = ocorrenciaMoradorService;
        this.relatorioService = relatorioService;
    }

    public List<MinhaUnidade> listarMinhasUnidades(Long condominioId, Usuario usuario) {
        Stream<Unidade> ondeMora = usuario.getPerfil() == Perfil.MORADOR
                ? vinculoMoradorRepository.findByMoradorIdAndDataSaidaIsNull(usuario.getId()).stream()
                        .map(VinculoMorador::getUnidade)
                : Stream.empty();

        Stream<Unidade> ondeEhDono = usuario.getPerfil() == Perfil.PROPRIETARIO
                ? unidadeRepository.findByProprietarioId(usuario.getId()).stream()
                : Stream.empty();

        Map<Long, Unidade> candidatas = Stream.concat(ondeMora, ondeEhDono)
                .filter(u -> u.getBloco().getCondominio().getId().equals(condominioId))
                .collect(Collectors.toMap(Unidade::getId, u -> u, (a, b) -> a, LinkedHashMap::new));

        return candidatas.values().stream()
                .map(u -> new MinhaUnidade(u, moradiaService.moraNaUnidade(usuario, u.getId()), ehDono(usuario, u)))
                .filter(m -> m.mora() || m.dono())
                .sorted(Comparator.comparing((MinhaUnidade m) -> m.unidade().getBloco().getNome())
                        .thenComparing(m -> m.unidade().getNumero()))
                .toList();
    }

    public Painel montarPainel(Long condominioId, Long unidadeId, Usuario usuario, LocalDate competenciaInformada) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .filter(u -> u.getBloco().getCondominio().getId().equals(condominioId))
                .orElseThrow(() -> new RegraDeNegocioException("Acesso negado a esta unidade."));

        boolean mora = moradiaService.moraNaUnidade(usuario, unidade.getId());
        boolean dono = ehDono(usuario, unidade);

        if (!mora && !dono) {
            throw new RegraDeNegocioException("Acesso negado a esta unidade.");
        }

        List<String> papeis = new ArrayList<>();
        if (mora) {
            papeis.add(PAPEL_MORADOR);
        }
        if (dono) {
            papeis.add(PAPEL_PROPRIETARIO);
        }

        LocalDate competencia = competenciaInformada != null
                ? competenciaInformada.withDayOfMonth(1)
                : LocalDate.now().minusMonths(1).withDayOfMonth(1);

        List<CobrancaDoPainel> cobrancas = montarCobrancas(unidade, usuario, dono);
        List<Reserva> reservas = mora ? reservaService.listarDaUnidade(unidade.getId()) : null;
        List<OcorrenciaMorador> ocorrencias = ocorrenciaMoradorService.listarDaUnidade(unidade, usuario);
        RelatorioPeriodoDTO prestacaoContas = relatorioService.gerarRelatorio(condominioId, competencia);

        return new Painel(
                unidade,
                papeis,
                new Permissoes(mora, mora, true, true),
                montarAvisos(condominioId, unidade, usuario),
                cobrancas,
                resumirSituacao(cobrancas),
                reservas,
                ocorrencias,
                competencia,
                prestacaoContas);
    }

    private List<AvisoDoPainel> montarAvisos(Long condominioId, Unidade unidade, Usuario usuario) {
        Map<Long, AvisoDoPainel> avisos = new LinkedHashMap<>();

        avisoService.listarAvisosDoUsuario(condominioId, usuario).stream()
                .filter(item -> seAplicaAUnidade(item.aviso(), unidade))
                .forEach(item -> avisos.put(item.aviso().getId(),
                        new AvisoDoPainel(item.aviso(), true, item.dataLeitura())));

        avisoRepository.findByCondominioIdOrderByDataCriacaoDesc(condominioId).stream()
                .filter(a -> a.getAlcance() == AlcanceAviso.CONDOMINIO)
                .filter(a -> !avisoService.estaVencido(a))
                .forEach(a -> avisos.putIfAbsent(a.getId(), new AvisoDoPainel(a, false, null)));

        return avisos.values().stream()
                .sorted(Comparator.comparing((AvisoDoPainel a) -> a.aviso().getDataCriacao()).reversed())
                .toList();
    }

    private boolean seAplicaAUnidade(Aviso aviso, Unidade unidade) {
        return switch (aviso.getAlcance()) {
            case CONDOMINIO -> true;
            case BLOCO -> aviso.getBloco() != null && aviso.getBloco().getId().equals(unidade.getBloco().getId());
            case UNIDADE -> aviso.getUnidade() != null && aviso.getUnidade().getId().equals(unidade.getId());
        };
    }

    private List<CobrancaDoPainel> montarCobrancas(Unidade unidade, Usuario usuario, boolean dono) {
        LocalDate hoje = LocalDate.now();

        return cobrancaRepository.findByUnidadeId(unidade.getId()).stream()
                .map(c -> {
                    RateioUnidade rateio = c.getRateioUnidade();
                    boolean responsavelOrdinario = mesmoUsuario(rateio.getResponsavelOrdinario(), usuario);
                    boolean responsavelExtraordinario = mesmoUsuario(rateio.getResponsavelExtraordinario(), usuario);

                    if (!dono && !responsavelOrdinario) {
                        return null;
                    }

                    boolean emAtraso = c.isEmAtraso();
                    return new CobrancaDoPainel(
                            c,
                            rateio.getRateioMensal().getCompetencia(),
                            responsavelOrdinario ? rateio.getValorOrdinario() : null,
                            responsavelExtraordinario || dono ? rateio.getValorExtraordinario() : null,
                            dono ? rateio.getValorTotal() : null,
                            emAtraso,
                            emAtraso ? ChronoUnit.DAYS.between(c.getVencimento(), hoje) : null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private SituacaoFinanceira resumirSituacao(List<CobrancaDoPainel> cobrancas) {
        List<CobrancaDoPainel> emAtraso = cobrancas.stream().filter(CobrancaDoPainel::emAtraso).toList();

        BigDecimal valorEmAtraso = emAtraso.stream()
                .map(c -> c.valorTotal() != null
                        ? c.valorTotal()
                        : Objects.requireNonNullElse(c.parteOrdinaria(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SituacaoFinanceira(emAtraso.isEmpty(), emAtraso.size(), valorEmAtraso);
    }

    private boolean ehDono(Usuario usuario, Unidade unidade) {
        return usuario.getPerfil() == Perfil.PROPRIETARIO && mesmoUsuario(unidade.getProprietario(), usuario);
    }

    private boolean mesmoUsuario(Usuario a, Usuario b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    public record MinhaUnidade(Unidade unidade, boolean mora, boolean dono) {
    }

    public record Permissoes(boolean autorizarVisita, boolean reservarAreaComum, boolean abrirOcorrencia,
                             boolean verPrestacaoContas) {
    }

    public record AvisoDoPainel(Aviso aviso, boolean confirmavel, LocalDateTime dataLeitura) {
    }

    public record CobrancaDoPainel(Cobranca cobranca, LocalDate competencia, BigDecimal parteOrdinaria,
                                   BigDecimal parteExtraordinaria, BigDecimal valorTotal, boolean emAtraso,
                                   Long diasAtraso) {
    }

    public record SituacaoFinanceira(boolean emDia, int cobrancasEmAtraso, BigDecimal valorEmAtraso) {
    }

    public record Painel(Unidade unidade, List<String> papeis, Permissoes permissoes, List<AvisoDoPainel> avisos,
                         List<CobrancaDoPainel> cobrancas, SituacaoFinanceira situacaoFinanceira,
                         List<Reserva> reservas, List<OcorrenciaMorador> ocorrencias, LocalDate competencia,
                         RelatorioPeriodoDTO prestacaoContas) {
    }
}
