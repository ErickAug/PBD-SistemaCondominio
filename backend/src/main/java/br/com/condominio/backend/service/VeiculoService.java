package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.Vaga;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.repository.UsuarioRepository;
import br.com.condominio.backend.repository.VagaRepository;
import br.com.condominio.backend.repository.VeiculoRepository;
import br.com.condominio.backend.util.PlacaUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final VagaRepository vagaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MoradiaService moradiaService;

    public VeiculoService(VeiculoRepository veiculoRepository,
                          VagaRepository vagaRepository,
                          UsuarioRepository usuarioRepository,
                          MoradiaService moradiaService) {
        this.veiculoRepository = veiculoRepository;
        this.vagaRepository = vagaRepository;
        this.usuarioRepository = usuarioRepository;
        this.moradiaService = moradiaService;
    }

    public Veiculo cadastrar(Unidade unidade, Long donoId, String placa, String modelo, String cor, Long vagaId) {
        String placaNormalizada = PlacaUtil.normalizar(placa);
        exigirTexto(modelo, "O modelo do veículo é obrigatório.");
        exigirTexto(cor, "A cor do veículo é obrigatória.");

        Condominio condominio = unidade.getBloco().getCondominio();

        veiculoRepository.findFirstByCondominioIdAndPlacaAndAtivoTrue(condominio.getId(), placaNormalizada)
                .ifPresent(existente -> {
                    throw new RegraDeNegocioException(
                            "Esta placa já está cadastrada na unidade " + descrever(existente.getUnidade()) + ".");
                });

        Usuario dono = resolverDono(donoId, unidade);
        Vaga vaga = resolverVaga(vagaId, unidade);

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(placaNormalizada);
        veiculo.setModelo(modelo.trim());
        veiculo.setCor(cor.trim());
        veiculo.setDono(dono);
        veiculo.setUnidade(unidade);
        veiculo.setVaga(vaga);
        veiculo.setCondominio(condominio);
        veiculo.setAtivo(true);

        return veiculoRepository.save(veiculo);
    }

    public void desativar(Long veiculoId, Long unidadeIdEsperada) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .filter(v -> v.getUnidade().getId().equals(unidadeIdEsperada))
                .orElseThrow(() -> new RegraDeNegocioException("Veículo não encontrado nesta unidade."));

        if (!veiculo.isAtivo()) {
            throw new RegraDeNegocioException("Este veículo já está desativado.");
        }

        inativar(veiculo);
        veiculoRepository.save(veiculo);
    }

    public void desativarTodosDaUnidade(Long unidadeId) {
        List<Veiculo> ativos = veiculoRepository.findByUnidadeIdAndAtivoTrue(unidadeId);
        ativos.forEach(this::inativar);
        veiculoRepository.saveAll(ativos);
    }

    public void desativarDoDonoNaUnidade(Long unidadeId, Long donoId) {
        List<Veiculo> doDono = veiculoRepository.findByUnidadeIdAndDonoIdAndAtivoTrue(unidadeId, donoId);
        doDono.forEach(this::inativar);
        veiculoRepository.saveAll(doDono);
    }

    public List<Veiculo> listarAtivosDaUnidade(Long unidadeId) {
        return veiculoRepository.findByUnidadeIdAndAtivoTrue(unidadeId);
    }

    public Optional<Veiculo> buscarPorPlaca(Long condominioId, String placa) {
        String placaNormalizada = PlacaUtil.normalizar(placa);
        return veiculoRepository.findFirstByCondominioIdAndPlacaAndAtivoTrue(condominioId, placaNormalizada);
    }

    public Map<Long, Veiculo> buscarPorVagas(List<Long> vagaIds) {
        if (vagaIds.isEmpty()) {
            return Map.of();
        }
        return veiculoRepository.findByVagaIdIn(vagaIds).stream()
                .collect(Collectors.toMap(v -> v.getVaga().getId(), v -> v));
    }

    private Usuario resolverDono(Long donoId, Unidade unidade) {
        if (donoId == null) {
            throw new RegraDeNegocioException("O dono do veículo é obrigatório.");
        }

        return usuarioRepository.findById(donoId)
                .filter(u -> moradiaService.moraNaUnidade(u, unidade.getId()))
                .orElseThrow(() -> new RegraDeNegocioException(
                        "O dono do veículo deve ser um morador ativo desta unidade ou o proprietário que mora nela."));
    }

    private Vaga resolverVaga(Long vagaId, Unidade unidade) {
        if (vagaId == null) {
            return null;
        }

        Vaga vaga = vagaRepository.findById(vagaId)
                .filter(v -> v.getUnidade().getId().equals(unidade.getId()))
                .orElseThrow(() -> new RegraDeNegocioException("Vaga não encontrada nesta unidade."));

        if (veiculoRepository.existsByVagaId(vaga.getId())) {
            throw new RegraDeNegocioException("Esta vaga já está ocupada por outro veículo.");
        }

        return vaga;
    }

    private void inativar(Veiculo veiculo) {
        veiculo.setAtivo(false);
        veiculo.setVaga(null);
    }

    private String descrever(Unidade unidade) {
        return unidade.getBloco().getNome() + " / unidade " + unidade.getNumero();
    }

    private void exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException(mensagem);
        }
    }
}