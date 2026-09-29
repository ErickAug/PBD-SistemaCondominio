package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.VinculoMorador;
import br.com.condominio.backend.model.enums.Perfil;
import br.com.condominio.backend.repository.UsuarioRepository;
import br.com.condominio.backend.repository.VinculoMoradorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VinculoMoradorService {

    private final VinculoMoradorRepository vinculoMoradorRepository;
    private final UsuarioRepository usuarioRepository;

    public VinculoMoradorService(VinculoMoradorRepository vinculoMoradorRepository,
                                 UsuarioRepository usuarioRepository) {
        this.vinculoMoradorRepository = vinculoMoradorRepository;
        this.usuarioRepository = usuarioRepository;
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

    public void encerrar(Long vinculoId, Long unidadeIdEsperada, LocalDate dataSaida) {
        VinculoMorador vinculo = vinculoMoradorRepository.findById(vinculoId)
                .orElseThrow(() -> new RegraDeNegocioException("Vínculo não encontrado."));

        if (!vinculo.getUnidade().getId().equals(unidadeIdEsperada)) {
            throw new RegraDeNegocioException("Este vínculo não pertence a esta unidade.");
        }

        if (vinculo.getDataSaida() != null) {
            throw new RegraDeNegocioException("Este vínculo já foi encerrado anteriormente.");
        }

        vinculo.setDataSaida(dataSaida);
        vinculoMoradorRepository.save(vinculo);
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