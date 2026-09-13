package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.CondominioRequestDTO;
import br.com.condominio.backend.dto.CondominioResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Condominio;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.CondominioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/administradoras/{administradoraId}/condominios")
public class CondominioController {

    private final CondominioService condominioService;

    public CondominioController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @PostMapping
    public ResponseEntity<CondominioResponseDTO> cadastrar(@PathVariable Long administradoraId,
                                                           @RequestBody CondominioRequestDTO dto,
                                                           @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarAcessoAdministradora(administradoraId, usuarioAutenticado);

        Condominio condominio = new Condominio();
        condominio.setNome(dto.nome());
        condominio.setEndereco(dto.endereco());
        condominio.setCnpj(dto.cnpj());
        condominio.setSituacao(dto.situacao());

        Condominio salvo = condominioService.cadastrar(condominio, administradoraId);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(salvo));
    }

    @GetMapping
    public ResponseEntity<List<CondominioResponseDTO>> listar(@PathVariable Long administradoraId,
                                                              @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarAcessoAdministradora(administradoraId, usuarioAutenticado);

        List<CondominioResponseDTO> lista = condominioService.listarPorAdministradora(administradoraId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(lista);
    }

    private void validarAcessoAdministradora(Long administradoraIdDaUrl, UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdDoToken = usuarioAutenticado.getUsuario().getAdministradora() != null
                ? usuarioAutenticado.getUsuario().getAdministradora().getId()
                : null;

        if (administradoraIdDoToken == null || !administradoraIdDoToken.equals(administradoraIdDaUrl)) {
            throw new RegraDeNegocioException("Acesso negado.");
        }
    }

    private CondominioResponseDTO toResponseDTO(Condominio condominio) {
        return new CondominioResponseDTO(
                condominio.getId(),
                condominio.getNome(),
                condominio.getEndereco(),
                condominio.getCnpj(),
                condominio.getSituacao(),
                condominio.getAdministradora().getId()
        );
    }
}