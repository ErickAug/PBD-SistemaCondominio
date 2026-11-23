package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.AutorizacaoVisita;
import br.com.condominio.backend.model.AutorizadoPermanente;
import br.com.condominio.backend.model.RegistroAcesso;
import br.com.condominio.backend.model.Unidade;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.model.Veiculo;
import br.com.condominio.backend.model.enums.OrigemRegistroAcesso;
import br.com.condominio.backend.repository.AutorizacaoVisitaRepository;
import br.com.condominio.backend.repository.AutorizadoPermanenteRepository;
import br.com.condominio.backend.repository.RegistroAcessoRepository;
import br.com.condominio.backend.repository.UnidadeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistroAcessoService {

    private final RegistroAcessoRepository registroAcessoRepository;
    private final UnidadeRepository unidadeRepository;
    private final VeiculoService veiculoService;
    private final AutorizacaoVisitaRepository autorizacaoVisitaRepository;
    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final AutorizadoPermanenteRepository autorizadoPermanenteRepository;
    private final AutorizadoPermanenteService autorizadoPermanenteService;

    public RegistroAcessoService(RegistroAcessoRepository registroAcessoRepository,
                                  UnidadeRepository unidadeRepository,
                                  VeiculoService veiculoService,
                                  AutorizacaoVisitaRepository autorizacaoVisitaRepository,
                                  AutorizacaoVisitaService autorizacaoVisitaService,
                                  AutorizadoPermanenteRepository autorizadoPermanenteRepository,
                                  AutorizadoPermanenteService autorizadoPermanenteService) {
        this.registroAcessoRepository = registroAcessoRepository;
        this.unidadeRepository = unidadeRepository;
        this.veiculoService = veiculoService;
        this.autorizacaoVisitaRepository = autorizacaoVisitaRepository;
        this.autorizacaoVisitaService = autorizacaoVisitaService;
        this.autorizadoPermanenteRepository = autorizadoPermanenteRepository;
        this.autorizadoPermanenteService = autorizadoPermanenteService;
    }

    public RegistroAcesso registrarManual(Long condominioId, Long unidadeId, String nome, String documento,
                                           String tipo, String quemAutorizou, String placa,
                                           Usuario porteiro) {
        Veiculo veiculo = null;
        Unidade unidade = null;

        if (placa != null && !placa.isBlank()) {
            veiculo = veiculoService.buscarPorPlaca(condominioId, placa).orElse(null);
            if (veiculo != null) {
                unidade = veiculo.getUnidade();
            }
        }

        if (unidade == null) {
            if (unidadeId == null) {
                throw new RegraDeNegocioException("Informe a unidade ou uma placa já cadastrada.");
            }
            unidade = buscarUnidadeDoCondominio(condominioId, unidadeId);
        }

        if (quemAutorizou == null || quemAutorizou.isBlank()) {
            throw new RegraDeNegocioException(
                    "Sem autorização prévia, registre quem, na unidade, liberou a entrada.");
        }

        validarNaoEstaDentro(condominioId, documento);

        RegistroAcesso registro = new RegistroAcesso();
        registro.setUnidade(unidade);
        registro.setNome(nome);
        registro.setDocumento(documento);
        registro.setTipo(tipo);
        registro.setOrigem(OrigemRegistroAcesso.MANUAL);
        registro.setQuemAutorizou(quemAutorizou);
        registro.setVeiculo(veiculo);
        registro.setPorteiro(porteiro);
        registro.setHoraEntrada(LocalDateTime.now());

        return registroAcessoRepository.save(registro);
    }

    public RegistroAcesso registrarPorAutorizacaoVisita(Long condominioId, Long autorizacaoVisitaId,
                                                         Usuario porteiro) {
        AutorizacaoVisita autorizacao = autorizacaoVisitaRepository.findById(autorizacaoVisitaId)
                .orElseThrow(() -> new RegraDeNegocioException("Autorização não encontrada."));

        validarNaoEstaDentro(condominioId, autorizacao.getDocumento());

        autorizacaoVisitaService.confirmarUso(condominioId, autorizacaoVisitaId);

        RegistroAcesso registro = new RegistroAcesso();
        registro.setUnidade(autorizacao.getUnidade());
        registro.setNome(autorizacao.getNome());
        registro.setDocumento(autorizacao.getDocumento());
        registro.setTipo(autorizacao.getTipo());
        registro.setOrigem(OrigemRegistroAcesso.AUTORIZACAO_VISITA);
        registro.setQuemAutorizou("Autorização prévia cadastrada pelo morador");
        registro.setAutorizacaoVisita(autorizacao);
        registro.setPorteiro(porteiro);
        registro.setHoraEntrada(LocalDateTime.now());

        return registroAcessoRepository.save(registro);
    }

    public RegistroAcesso registrarPorAutorizadoPermanente(Long condominioId, Long autorizadoPermanenteId,
                                                            Usuario porteiro) {
        AutorizadoPermanente autorizado = autorizadoPermanenteRepository.findById(autorizadoPermanenteId)
                .orElseThrow(() -> new RegraDeNegocioException("Autorizado permanente não encontrado."));

        if (!autorizado.getUnidade().getBloco().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Autorizado permanente não encontrado.");
        }

        if (!autorizadoPermanenteService.estaLiberadoHoje(autorizado)) {
            throw new RegraDeNegocioException("Esta autorização permanente não é válida hoje.");
        }

        validarNaoEstaDentro(condominioId, autorizado.getDocumento());

        RegistroAcesso registro = new RegistroAcesso();
        registro.setUnidade(autorizado.getUnidade());
        registro.setNome(autorizado.getNome());
        registro.setDocumento(autorizado.getDocumento());
        registro.setTipo(autorizado.getTipo());
        registro.setOrigem(OrigemRegistroAcesso.AUTORIZADO_PERMANENTE);
        registro.setQuemAutorizou("Autorizado permanente");
        registro.setAutorizadoPermanente(autorizado);
        registro.setPorteiro(porteiro);
        registro.setHoraEntrada(LocalDateTime.now());

        return registroAcessoRepository.save(registro);
    }

    public List<RegistroAcesso> listarPorCondominio(Long condominioId) {
        return registroAcessoRepository.listarPorCondominio(condominioId);
    }

    private Unidade buscarUnidadeDoCondominio(Long condominioId, Long unidadeId) {
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Unidade não encontrada."));

        if (!unidade.getBloco().getCondominio().getId().equals(condominioId)) {
            throw new RegraDeNegocioException("Esta unidade não pertence a este condomínio.");
        }

        return unidade;
    }

    private void validarNaoEstaDentro(Long condominioId, String documento) {
        if (registroAcessoRepository.existeEntradaAbertaPorDocumento(condominioId, documento)) {
            throw new RegraDeNegocioException(
                    "Esta pessoa já está dentro do condomínio e ainda não teve saída registrada.");
        }
    }
}
