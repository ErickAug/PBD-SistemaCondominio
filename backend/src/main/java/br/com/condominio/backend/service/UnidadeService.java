package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Bloco;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.OcupacaoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import br.com.condominio.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class UnidadeService {

    private static final BigDecimal CEM_POR_CENTO = new BigDecimal("100");

    private final OcupacaoRepository ocupacaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;

    public UnidadeService(UnidadeRepository unidadeRepository, UsuarioRepository usuarioRepository, OcupacaoRepository ocupacaoRepository) {
        this.unidadeRepository = unidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.ocupacaoRepository = ocupacaoRepository;
    }

    public Unidade cadastrar(Unidade unidade, Bloco blocoJaValidado) {
        validarFracaoIdeal(unidade.getFracaoIdeal());

        if (unidadeRepository.existsByNumeroAndBlocoId(unidade.getNumero(), blocoJaValidado.getId())) {
            throw new RegraDeNegocioException("Já existe uma unidade com este número neste bloco.");
        }

        unidade.setBloco(blocoJaValidado);
        return unidadeRepository.save(unidade);
    }

    public Unidade definirProprietario(Unidade unidade, Long proprietarioId, Long condominioId) {
        if (proprietarioId == null) {
            unidade.setProprietario(null);
            return unidadeRepository.save(unidade);
        }

        Usuario proprietario = usuarioRepository.findById(proprietarioId)
                .orElseThrow(() -> new RegraDeNegocioException("Proprietário não encontrado."));

        if (proprietario.getPerfil() != Perfil.PROPRIETARIO) {
            throw new RegraDeNegocioException("Usuário informado não tem perfil PROPRIETARIO.");
        }

        if (!proprietario.getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("O proprietário não pertence a este condomínio.");
        }

        unidade.setProprietario(proprietario);
        return unidadeRepository.save(unidade);
    }

    public void excluir(Unidade unidadeJaValidada) {
        if (unidadeJaValidada.getProprietario() != null) {
            throw new RegraDeNegocioException("Não é possível excluir: existe proprietário vinculado a esta unidade.");
        }

        if (ocupacaoRepository.existsByUnidadeId(unidadeJaValidada.getId())) {
            throw new RegraDeNegocioException("Não é possível excluir: existe histórico de ocupação vinculado a esta unidade.");
        }

        // TODO: quando a entidade Cobranca existir, checar tambem cobrancas vinculadas antes de excluir.

        unidadeRepository.deleteById(unidadeJaValidada.getId());
    }

    public List<Unidade> listarPorBloco(Long blocoId) {
        return unidadeRepository.findByBlocoId(blocoId);
    }

    public Set<Long> identificarUnidadesOcupadas(List<Long> unidadeIds) {
        // TODO: reimplementar usando a entidade Ocupacao (T03) - por enquanto, nenhuma unidade aparece como ocupada.
        return Set.of();
    }

    public StatusFracaoIdeal calcularStatusFracao(Long condominioId) {
        BigDecimal soma = unidadeRepository.somarFracaoIdealPorCondominio(condominioId);
        BigDecimal diferenca = CEM_POR_CENTO.subtract(soma);
        boolean fechaEm100 = diferenca.compareTo(BigDecimal.ZERO) == 0;

        return new StatusFracaoIdeal(soma, diferenca, fechaEm100);
    }

    public Unidade buscarValidandoBloco(Long unidadeId, Long blocoId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

        if (!unidade.getBloco().getId().equals(blocoId)) {
            throw new RegraDeNegocioException("Esta unidade não pertence a este bloco.");
        }

        return unidade;
    }

    private void validarFracaoIdeal(BigDecimal fracaoIdeal) {
        if (fracaoIdeal == null || fracaoIdeal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RegraDeNegocioException("A fração ideal não pode ser negativa.");
        }
        if (fracaoIdeal.compareTo(CEM_POR_CENTO) > 0) {
            throw new RegraDeNegocioException("A fração ideal não pode ser maior que 100%.");
        }
    }

    public record StatusFracaoIdeal(BigDecimal somaAtual, BigDecimal diferencaParaFechar, boolean fechaEm100) {
    }
}