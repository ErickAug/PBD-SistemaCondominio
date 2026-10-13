package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.UsuarioRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class VinculoMoradorService {

    private final VinculoMoradorRepository vinculoMoradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final VeiculoService veiculoService;

    public VinculoMoradorService(VinculoMoradorRepository vinculoMoradorRepository,
                                 UsuarioRepository usuarioRepository,
                                 VeiculoService veiculoService) {
        this.vinculoMoradorRepository = vinculoMoradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.veiculoService = veiculoService;
    }

    public VinculoMorador registrar(Unidade unidade, Long moradorId, LocalDate dataEntrada) {
        Usuario morador = usuarioRepository.findById(moradorId)
                .orElseThrow(() -> new RegraDeNegocioException("Morador não encontrado."));

        if (morador.getPerfil() != Perfil.MORADOR) {
            throw new RegraDeNegocioException("Usuário informado não tem perfil MORADOR.");
        }

        if (!morador.getCondominio().getId().equals(unidade.getBloco().getCondominio().getId())) {
            throw new RegraDeNegocioException("O morador não pertence a este condomínio.");
        }

        boolean jaVinculadoNestaUnidade = vinculoMoradorRepository
                .findByUnidadeIdAndDataSaidaIsNull(unidade.getId())
                .stream()
                .anyMatch(v -> v.getMorador().getId().equals(moradorId));

        if (jaVinculadoNestaUnidade) {
            throw new RegraDeNegocioException("Este morador já está vinculado ativamente a esta unidade.");
        }

        VinculoMorador vinculo = new VinculoMorador();
        vinculo.setUnidade(unidade);
        vinculo.setMorador(morador);
        vinculo.setDataEntrada(dataEntrada);

        return vinculoMoradorRepository.save(vinculo);
    }

    @Transactional
    public void encerrar(Long vinculoId, Long unidadeIdEsperada, LocalDate dataSaida) {
        if (dataSaida == null) {
            throw new RegraDeNegocioException("A data de saída é obrigatória.");
        }

        VinculoMorador vinculo = vinculoMoradorRepository.findById(vinculoId)
                .filter(v -> v.getUnidade().getId().equals(unidadeIdEsperada))
                .orElseThrow(() -> new RegraDeNegocioException("Vínculo não encontrado nesta unidade."));

        if (vinculo.getDataSaida() != null) {
            throw new RegraDeNegocioException("Este vínculo já foi encerrado anteriormente.");
        }

        if (dataSaida.isBefore(vinculo.getDataEntrada())) {
            throw new RegraDeNegocioException("A data de saída não pode ser anterior à data de entrada.");
        }

        vinculo.setDataSaida(dataSaida);
        vinculoMoradorRepository.save(vinculo);

        veiculoService.desativarDoDonoNaUnidade(unidadeIdEsperada, vinculo.getMorador().getId());
    }

    public void encerrarTodosAtivosDaUnidade(Long unidadeId, LocalDate dataSaida) {
        List<VinculoMorador> ativos = vinculoMoradorRepository.findByUnidadeIdAndDataSaidaIsNull(unidadeId);
        ativos.forEach(v -> v.setDataSaida(dataSaida));
        vinculoMoradorRepository.saveAll(ativos);
    }

    public List<VinculoMorador> listarAtivos(Long unidadeId) {
        return vinculoMoradorRepository.findByUnidadeIdAndDataSaidaIsNull(unidadeId);
    }

    public List<VinculoMorador> listarHistorico(Long unidadeId) {
        return vinculoMoradorRepository.findByUnidadeIdOrderByDataEntradaDesc(unidadeId);
    }
}