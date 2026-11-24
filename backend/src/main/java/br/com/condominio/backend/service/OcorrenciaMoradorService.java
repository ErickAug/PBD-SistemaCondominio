package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.OcorrenciaMorador;
import br.com.condominio.backend.model.OcorrenciaMoradorHistorico;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.model.enums.SituacaoOcorrenciaMorador;
import br.com.condominio.backend.model.enums.TipoOcorrenciaMorador;
import br.com.condominio.backend.repository.OcorrenciaMoradorHistoricoRepository;
import br.com.condominio.backend.repository.OcorrenciaMoradorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class OcorrenciaMoradorService {

    public static final int TAMANHO_MAXIMO_FOTO = 3_000_000;

    private static final Set<SituacaoOcorrenciaMorador> SITUACOES_ENCERRADAS =
            Set.of(SituacaoOcorrenciaMorador.RESOLVIDA, SituacaoOcorrenciaMorador.RECUSADA);

    private final OcorrenciaMoradorRepository ocorrenciaMoradorRepository;
    private final OcorrenciaMoradorHistoricoRepository historicoRepository;
    private final MoradiaService moradiaService;

    public OcorrenciaMoradorService(OcorrenciaMoradorRepository ocorrenciaMoradorRepository,
                                    OcorrenciaMoradorHistoricoRepository historicoRepository,
                                    MoradiaService moradiaService) {
        this.ocorrenciaMoradorRepository = ocorrenciaMoradorRepository;
        this.historicoRepository = historicoRepository;
        this.moradiaService = moradiaService;
    }

    @Transactional
    public OcorrenciaMorador abrir(Unidade unidade, Usuario autor, String assunto, TipoOcorrenciaMorador tipo,
                                   String descricao, String foto) {
        resolverAcesso(unidade, autor);

        if (assunto == null || assunto.isBlank()) {
            throw new RegraDeNegocioException("Informe o assunto da ocorrência.");
        }
        if (tipo == null) {
            throw new RegraDeNegocioException("Informe o tipo da ocorrência: RECLAMACAO, SOLICITACAO ou DEFEITO.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("Informe a descrição da ocorrência.");
        }

        LocalDateTime agora = agora();

        OcorrenciaMorador ocorrencia = new OcorrenciaMorador();
        ocorrencia.setCondominio(unidade.getBloco().getCondominio());
        ocorrencia.setUnidade(unidade);
        ocorrencia.setAssunto(assunto.trim());
        ocorrencia.setTipo(tipo);
        ocorrencia.setDescricao(descricao.trim());
        ocorrencia.setFoto(validarFoto(foto));
        ocorrencia.setSituacao(SituacaoOcorrenciaMorador.ABERTA);
        ocorrencia.setAbertoPor(autor);
        ocorrencia.setDataAbertura(agora);

        OcorrenciaMorador salva = ocorrenciaMoradorRepository.save(ocorrencia);

        registrarHistorico(salva, null, SituacaoOcorrenciaMorador.ABERTA, null, autor, agora);

        return salva;
    }

    public List<OcorrenciaMorador> listarDaUnidade(Unidade unidade, Usuario usuario) {
        if (resolverAcesso(unidade, usuario) == Acesso.MORA_NA_UNIDADE) {
            return ocorrenciaMoradorRepository.findByUnidadeIdOrderByDataAberturaDesc(unidade.getId());
        }
        return ocorrenciaMoradorRepository.findByUnidadeIdAndAbertoPorIdOrderByDataAberturaDesc(
                unidade.getId(), usuario.getId());
    }

    public OcorrenciaMorador detalharDaUnidade(Unidade unidade, Usuario usuario, Long ocorrenciaId) {
        Acesso acesso = resolverAcesso(unidade, usuario);

        OcorrenciaMorador ocorrencia = ocorrenciaMoradorRepository.findByIdAndUnidadeId(ocorrenciaId, unidade.getId())
                .orElseThrow(() -> new RegraDeNegocioException("Ocorrência não encontrada."));

        if (acesso == Acesso.PROPRIETARIO_QUE_NAO_MORA
                && !ocorrencia.getAbertoPor().getId().equals(usuario.getId())) {
            throw new RegraDeNegocioException("Ocorrência não encontrada.");
        }

        return ocorrencia;
    }

    public List<OcorrenciaMorador> listarFila(Long condominioId, SituacaoOcorrenciaMorador situacao) {
        Comparator<OcorrenciaMorador> porPrioridade = Comparator.comparingInt(o -> prioridade(o.getSituacao()));
        Comparator<OcorrenciaMorador> maisAntigaPrimeiro = Comparator.comparing(OcorrenciaMorador::getDataAbertura);

        return ocorrenciaMoradorRepository.findByCondominioId(condominioId).stream()
                .filter(o -> situacao == null || o.getSituacao() == situacao)
                .sorted(porPrioridade.thenComparing((a, b) -> estaEncerrada(a)
                        ? maisAntigaPrimeiro.compare(b, a)
                        : maisAntigaPrimeiro.compare(a, b)))
                .toList();
    }

    public OcorrenciaMorador detalharNoCondominio(Long condominioId, Long ocorrenciaId) {
        return ocorrenciaMoradorRepository.findByIdAndCondominioId(ocorrenciaId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Ocorrência não encontrada."));
    }

    @Transactional
    public OcorrenciaMorador responder(Long condominioId, Long ocorrenciaId, Usuario responsavel,
                                       SituacaoOcorrenciaMorador novaSituacao, String resposta) {
        OcorrenciaMorador ocorrencia = detalharNoCondominio(condominioId, ocorrenciaId);

        if (novaSituacao == null) {
            throw new RegraDeNegocioException("Informe a nova situação: EM_ANALISE, RESOLVIDA ou RECUSADA.");
        }
        if (resposta == null || resposta.isBlank()) {
            throw new RegraDeNegocioException("Informe o texto de resposta para o morador.");
        }
        if (estaEncerrada(ocorrencia)) {
            throw new RegraDeNegocioException("Esta ocorrência já foi encerrada e não pode mudar de situação.");
        }
        if (novaSituacao == SituacaoOcorrenciaMorador.ABERTA) {
            throw new RegraDeNegocioException("Uma ocorrência não pode voltar para a situação ABERTA.");
        }
        if (novaSituacao == ocorrencia.getSituacao()) {
            throw new RegraDeNegocioException("A ocorrência já está nesta situação.");
        }

        LocalDateTime agora = agora();
        SituacaoOcorrenciaMorador anterior = ocorrencia.getSituacao();

        ocorrencia.setSituacao(novaSituacao);
        ocorrencia.setRespondidoPor(responsavel);
        ocorrencia.setUltimaResposta(resposta.trim());
        ocorrencia.setDataUltimaResposta(agora);

        OcorrenciaMorador salva = ocorrenciaMoradorRepository.save(ocorrencia);

        registrarHistorico(salva, anterior, novaSituacao, resposta.trim(), responsavel, agora);

        return salva;
    }

    public List<OcorrenciaMoradorHistorico> listarHistorico(Long ocorrenciaId) {
        return historicoRepository.findByOcorrenciaIdOrderByDataHoraAscIdAsc(ocorrenciaId);
    }

    public boolean estaEncerrada(OcorrenciaMorador ocorrencia) {
        return SITUACOES_ENCERRADAS.contains(ocorrencia.getSituacao());
    }

    private Acesso resolverAcesso(Unidade unidade, Usuario usuario) {
        if (moradiaService.moraNaUnidade(usuario, unidade.getId())) {
            return Acesso.MORA_NA_UNIDADE;
        }

        boolean ehODono = usuario.getPerfil() == Perfil.PROPRIETARIO
                && unidade.getProprietario() != null
                && unidade.getProprietario().getId().equals(usuario.getId());

        if (ehODono) {
            return Acesso.PROPRIETARIO_QUE_NAO_MORA;
        }

        throw new RegraDeNegocioException("Acesso negado a esta unidade.");
    }

    private String validarFoto(String foto) {
        if (foto == null || foto.isBlank()) {
            return null;
        }
        if (!foto.startsWith("data:image/")) {
            throw new RegraDeNegocioException("A foto deve ser uma imagem em base64 no formato data:image/...");
        }
        if (foto.length() > TAMANHO_MAXIMO_FOTO) {
            throw new RegraDeNegocioException("A foto é grande demais. O limite é de cerca de 2 MB.");
        }
        return foto;
    }

    private void registrarHistorico(OcorrenciaMorador ocorrencia, SituacaoOcorrenciaMorador anterior,
                                    SituacaoOcorrenciaMorador nova, String resposta, Usuario responsavel,
                                    LocalDateTime quando) {
        OcorrenciaMoradorHistorico historico = new OcorrenciaMoradorHistorico();
        historico.setOcorrencia(ocorrencia);
        historico.setSituacaoAnterior(anterior);
        historico.setSituacaoNova(nova);
        historico.setResposta(resposta);
        historico.setResponsavel(responsavel);
        historico.setDataHora(quando);
        historicoRepository.save(historico);
    }

    private int prioridade(SituacaoOcorrenciaMorador situacao) {
        return switch (situacao) {
            case ABERTA -> 0;
            case EM_ANALISE -> 1;
            case RESOLVIDA, RECUSADA -> 2;
        };
    }

    private LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private enum Acesso {
        MORA_NA_UNIDADE,
        PROPRIETARIO_QUE_NAO_MORA
    }
}
