package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.UsuarioRequestDTO;
import br.com.condominio.backend.dto.UsuarioResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.security.UsuarioDetailsImpl;
import br.com.condominio.backend.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administradoras/{administradoraId}/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@PathVariable Long administradoraId,
                                                        @RequestBody UsuarioRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioDetailsImpl usuarioAutenticado) {
        validarAcessoAdministradora(administradoraId, usuarioAutenticado);

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setUsuario(dto.usuario());
        usuario.setSenha(dto.senha());
        usuario.setPerfil(dto.perfil());

        Usuario salvo = usuarioService.cadastrar(usuario, dto.condominioId(), administradoraId);

        UsuarioResponseDTO resposta = new UsuarioResponseDTO(
                salvo.getId(),
                salvo.getNome(),
                salvo.getUsuario(),
                salvo.getPerfil(),
                salvo.getAdministradora() != null ? salvo.getAdministradora().getId() : null,
                salvo.getCondominio() != null ? salvo.getCondominio().getId() : null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    private void validarAcessoAdministradora(Long administradoraIdDaUrl, UsuarioDetailsImpl usuarioAutenticado) {
        Long administradoraIdDoToken = usuarioAutenticado.getUsuario().getAdministradora() != null
                ? usuarioAutenticado.getUsuario().getAdministradora().getId()
                : null;

        if (administradoraIdDoToken == null || !administradoraIdDoToken.equals(administradoraIdDaUrl)) {
            throw new RegraDeNegocioException("Acesso negado.");
        }
    }
}