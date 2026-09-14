package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.repository.BlocoRepository;
import br.com.condominio.backend.repository.CondominioRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class UnidadeService {

    private static final BigDecimal CEM_POR_CENTO = new BigDecimal("100");

    private final UnidadeRepository unidadeRepository;
    private final BlocoRepository blocoRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioRepository usuarioRepository;

    public UnidadeService(UnidadeRepository unidadeRepository,
                          BlocoRepository blocoRepository,
                          CondominioRepository condominioRepository,
                          UsuarioRepository usuarioRepository) {
        this.unidadeRepository = unidadeRepository;
        this.blocoRepository = blocoRepository;
        this.condominioRepository = condominioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Unidade cadastrar(Unidade unidade, Long blocoId, Long administradoraIdAutenticado) {
        Bloco bloco = buscarBlocoValidandoTenant(blocoId, administradoraIdAutenticado);

        validarFracaoIdeal(unidade.getFracaoIdeal());

        if (unidadeRepository.existsByNumeroAndBlocoId(unidade.getNumero(), blocoId)) {
            throw new RegraDeNegocioException("Já existe uma unidade com este número neste bloco.");
        }

        unidade.setBloco(bloco);
        return unidadeRepository.save(unidade);
    }

    public void excluir(Long unidadeId, Long administradoraIdAutenticado) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

        if (!unidade.getBloco().getCondominio().getAdministradora().getId().equals(administradoraIdAutenticado)) {
            throw new RegraDeNegocioException("Acesso negado a esta unidade.");
        }

        if (usuarioRepository.existsByUnidadeId(unidadeId)) {
            throw new RegraDeNegocioException("Não é possível excluir: existe morador vinculado a esta unidade.");
        }

        // TODO: quando a entidade Cobranca existir, checar tambem cobrancas vinculadas antes de excluir.

        unidadeRepository.deleteById(unidadeId);
    }

    public List<Unidade> listarPorBloco(Long blocoId, Long administradoraIdAutenticado) {
        buscarBlocoValidandoTenant(blocoId, administradoraIdAutenticado);
        return unidadeRepository.findByBlocoId(blocoId);
    }

    public Set<Long> identificarUnidadesOcupadas(List<Long> unidadeIds) {
        if (unidadeIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(usuarioRepository.buscarIdsDeUnidadesOcupadas(unidadeIds));
    }

    public StatusFracaoIdeal calcularStatusFracao(Long condominioId, Long administradoraIdAutenticado) {
        Condominio condominio = condominioRepository.findById(condominioId)
                .orElseThrow(() -> new RegraDeNegocioException("Condomínio não encontrado."));

        if (!condominio.getAdministradora().getId().equals(administradoraIdAutenticado)) {
            throw new RegraDeNegocioException("Acesso negado a este condomínio.");
        }

        BigDecimal soma = unidadeRepository.somarFracaoIdealPorCondominio(condominioId);
        BigDecimal diferenca = CEM_POR_CENTO.subtract(soma);
        boolean fechaEm100 = diferenca.compareTo(BigDecimal.ZERO) == 0;

        return new StatusFracaoIdeal(soma, diferenca, fechaEm100);
    }

    private void validarFracaoIdeal(BigDecimal fracaoIdeal) {
        if (fracaoIdeal == null || fracaoIdeal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("A fração ideal não pode ser negativa.");
        }

        if (fracaoIdeal.compareTo(CEM_POR_CENTO) > 0) {
            throw new RegraDeNegocioException("A fração ideal não pode ser maior que 100%.");
        }
    }

    private Bloco buscarBlocoValidandoTenant(Long blocoId, Long administradoraIdAutenticado) {
        Bloco bloco = blocoRepository.findById(blocoId)
                .orElseThrow(() -> new RegraDeNegocioException("Bloco não encontrado."));

        if (!bloco.getCondominio().getAdministradora().getId().equals(administradoraIdAutenticado)) {
            throw new RegraDeNegocioException("Acesso negado a este bloco.");
        }

        return bloco;
    }

    public record StatusFracaoIdeal(BigDecimal somaAtual, BigDecimal diferencaParaFechar, boolean fechaEm100) {
    }
}