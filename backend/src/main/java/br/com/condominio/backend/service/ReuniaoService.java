package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Reuniao;
import br.com.condominio.backend.model.ReuniaoPautaItem;
import br.com.condominio.backend.model.ReuniaoPresenca;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.SituacaoReuniao;
import br.com.condominio.backend.repository.ReuniaoPautaItemRepository;
import br.com.condominio.backend.repository.ReuniaoPresencaRepository;
import br.com.condominio.backend.repository.ReuniaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReuniaoService {

    public static final int TAMANHO_MAXIMO_ARQUIVO_ATA = 7_000_000;

    private final ReuniaoRepository reuniaoRepository;
    private final ReuniaoPautaItemRepository pautaRepository;
    private final ReuniaoPresencaRepository presencaRepository;
    private final UnidadeRepository unidadeRepository;

    public ReuniaoService(ReuniaoRepository reuniaoRepository,
                          ReuniaoPautaItemRepository pautaRepository,
                          ReuniaoPresencaRepository presencaRepository,
                          UnidadeRepository unidadeRepository) {
        this.reuniaoRepository = reuniaoRepository;
        this.pautaRepository = pautaRepository;
        this.presencaRepository = presencaRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional
    public Reuniao convocar(Condominio condominioJaValidado, Usuario autor, String titulo, LocalDate data,
                            LocalTime hora, String local, List<String> pauta) {
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("Informe o título da reunião.");
        }
        if (data == null) {
            throw new RegraDeNegocioException("Informe a data da reunião.");
        }
        if (hora == null) {
            throw new RegraDeNegocioException("Informe a hora da reunião.");
        }
        if (local == null || local.isBlank()) {
            throw new RegraDeNegocioException("Informe o local da reunião.");
        }
        if (pauta == null || pauta.isEmpty()) {
            throw new RegraDeNegocioException("Informe a pauta com pelo menos um assunto.");
        }
        if (pauta.stream().anyMatch(item -> item == null || item.isBlank())) {
            throw new RegraDeNegocioException("Os itens da pauta não podem ficar em branco.");
        }

        Reuniao reuniao = new Reuniao();
        reuniao.setCondominio(condominioJaValidado);
        reuniao.setTitulo(titulo.trim());
        reuniao.setData(data);
        reuniao.setHora(hora);
        reuniao.setLocal(local.trim());
        reuniao.setSituacao(SituacaoReuniao.CONVOCADA);
        reuniao.setConvocadaPor(autor);
        reuniao.setDataPublicacao(agora());

        Reuniao salva = reuniaoRepository.save(reuniao);

        List<ReuniaoPautaItem> itens = new ArrayList<>();
        for (int i = 0; i < pauta.size(); i++) {
            ReuniaoPautaItem item = new ReuniaoPautaItem();
            item.setReuniao(salva);
            item.setOrdem(i + 1);
            item.setDescricao(pauta.get(i).trim());
            itens.add(item);
        }
        pautaRepository.saveAll(itens);

        return salva;
    }

    public List<Reuniao> listarHistorico(Long condominioId, LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial do período não pode ser depois da data final.");
        }

        return reuniaoRepository.findByCondominioIdOrderByDataDescHoraDesc(condominioId).stream()
                .filter(r -> inicio == null || !r.getData().isBefore(inicio))
                .filter(r -> fim == null || !r.getData().isAfter(fim))
                .toList();
    }

    public Reuniao buscarNoCondominio(Long condominioId, Long reuniaoId) {
        return reuniaoRepository.findByIdAndCondominioId(reuniaoId, condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Reunião não encontrada."));
    }

    public List<ReuniaoPautaItem> listarPauta(Long reuniaoId) {
        return pautaRepository.findByReuniaoIdOrderByOrdemAsc(reuniaoId);
    }

    public long contarItensPauta(Long reuniaoId) {
        return pautaRepository.countByReuniaoId(reuniaoId);
    }

    public List<ReuniaoPresenca> listarPresencas(Long reuniaoId) {
        return presencaRepository.findByReuniaoId(reuniaoId).stream()
                .sorted(Comparator.comparing((ReuniaoPresenca p) -> p.getUnidade().getBloco().getNome())
                        .thenComparing(p -> p.getUnidade().getNumero()))
                .toList();
    }

    public long contarPresencas(Long reuniaoId) {
        return presencaRepository.countByReuniaoId(reuniaoId);
    }

    public int contarUnidadesDoCondominio(Long condominioId) {
        return unidadeRepository.findByBlocoCondominioId(condominioId).size();
    }

    public ReuniaoPresenca registrarPresenca(Long condominioId, Long reuniaoId, Long unidadeId, String compareceu,
                                             Usuario registradoPor) {
        Reuniao reuniao = buscarNoCondominio(condominioId, reuniaoId);

        if (unidadeId == null) {
            throw new RegraDeNegocioException("Informe a unidade.");
        }
        if (compareceu == null || compareceu.isBlank()) {
            throw new RegraDeNegocioException("Informe quem compareceu pela unidade.");
        }
        if (reuniao.getSituacao() == SituacaoReuniao.REALIZADA) {
            throw new RegraDeNegocioException("A ata já foi anexada; a lista de presença não pode mais ser alterada.");
        }
        if (LocalDate.now().isBefore(reuniao.getData())) {
            throw new RegraDeNegocioException("A presença só pode ser registrada a partir do dia da reunião.");
        }

        Unidade unidade = unidadeRepository.findById(unidadeId)
                .filter(u -> u.getBloco().getCondominio().getId().equals(condominioId))
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada neste condomínio."));

        presencaRepository.findByReuniaoIdAndUnidadeId(reuniaoId, unidadeId).ifPresent(existente -> {
            throw new RegraDeNegocioException("A unidade " + unidade.getNumero() + " do bloco "
                    + unidade.getBloco().getNome() + " já tem presença registrada nesta reunião (compareceu: "
                    + existente.getCompareceu() + ").");
        });

        ReuniaoPresenca presenca = new ReuniaoPresenca();
        presenca.setReuniao(reuniao);
        presenca.setUnidade(unidade);
        presenca.setCompareceu(compareceu.trim());
        presenca.setRegistradoPor(registradoPor);
        presenca.setDataRegistro(agora());

        return presencaRepository.save(presenca);
    }

    public Reuniao anexarAta(Long condominioId, Long reuniaoId, String texto, String arquivo, Usuario autor) {
        Reuniao reuniao = buscarNoCondominio(condominioId, reuniaoId);

        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("Informe o texto da ata com as deliberações.");
        }
        if (reuniao.getSituacao() == SituacaoReuniao.REALIZADA) {
            throw new RegraDeNegocioException("Esta reunião já tem ata anexada.");
        }
        if (LocalDateTime.of(reuniao.getData(), reuniao.getHora()).isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("A ata só pode ser anexada depois que a reunião acontecer.");
        }

        reuniao.setAtaTexto(texto.trim());
        reuniao.setAtaArquivo(validarArquivo(arquivo));
        reuniao.setAtaAnexadaPor(autor);
        reuniao.setDataAnexoAta(agora());
        reuniao.setSituacao(SituacaoReuniao.REALIZADA);

        return reuniaoRepository.save(reuniao);
    }

    private String validarArquivo(String arquivo) {
        if (arquivo == null || arquivo.isBlank()) {
            return null;
        }
        if (!arquivo.startsWith("data:") || !arquivo.contains(";base64,")) {
            throw new RegraDeNegocioException(
                    "O arquivo da ata deve ser enviado em base64 no formato data:<tipo>;base64,...");
        }
        if (arquivo.length() > TAMANHO_MAXIMO_ARQUIVO_ATA) {
            throw new RegraDeNegocioException("O arquivo da ata é grande demais. O limite é de cerca de 5 MB.");
        }
        return arquivo;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
