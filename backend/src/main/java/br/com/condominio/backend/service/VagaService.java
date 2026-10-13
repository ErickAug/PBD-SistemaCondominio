package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Vaga;
import br.com.condominio.backend.model.enums.TipoVaga;
import br.com.condominio.backend.repository.VagaRepository;
import br.com.condominio.backend.repository.VeiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;
    private final VeiculoRepository veiculoRepository;

    public VagaService(VagaRepository vagaRepository, VeiculoRepository veiculoRepository) {
        this.vagaRepository = vagaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public Vaga cadastrar(Unidade unidade, String numero, TipoVaga tipo) {
        if (numero == null || numero.isBlank()) {
            throw new RegraDeNegocioException("O número da vaga é obrigatório.");
        }
        if (tipo == null) {
            throw new RegraDeNegocioException("O tipo da vaga é obrigatório (COBERTA ou DESCOBERTA).");
        }

        String numeroLimpo = numero.trim();
        Condominio condominio = unidade.getBloco().getCondominio();

        if (vagaRepository.existsByNumeroAndCondominioId(numeroLimpo, condominio.getId())) {
            throw new RegraDeNegocioException("Já existe uma vaga com este número neste condomínio.");
        }

        Vaga vaga = new Vaga();
        vaga.setNumero(numeroLimpo);
        vaga.setTipo(tipo);
        vaga.setUnidade(unidade);
        vaga.setCondominio(condominio);

        return vagaRepository.save(vaga);
    }

    public void excluir(Long vagaId, Long unidadeIdEsperada) {
        Vaga vaga = vagaRepository.findById(vagaId)
                .filter(v -> v.getUnidade().getId().equals(unidadeIdEsperada))
                .orElseThrow(() -> new RegraDeNegocioException("Vaga não encontrada nesta unidade."));

        if (veiculoRepository.existsByVagaId(vagaId)) {
            throw new RegraDeNegocioException("Não é possível excluir: existe veículo ocupando esta vaga.");
        }

        vagaRepository.delete(vaga);
    }

    public List<Vaga> listarPorUnidade(Long unidadeId) {
        return vagaRepository.findByUnidadeIdOrderByNumeroAsc(unidadeId);
    }

    public long contarPorUnidade(Long unidadeId) {
        return vagaRepository.countByUnidadeId(unidadeId);
    }
}