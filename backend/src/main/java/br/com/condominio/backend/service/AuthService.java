package br.com.condominio.backend.service;

import br.com.condominio.backend.dto.LoginResponseDTO;
import br.com.condominio.backend.exception.RegraDeNegocioException;
import br.com.condominio.backend.model.Usuario;
import br.com.condominio.backend.repository.UsuarioRepository;
import br.com.condominio.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponseDTO autenticar(String usuarioLogin, String senha) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuarioLogin, senha)
            );
        } catch (AuthenticationException excecaoDeAutenticacao) {
            throw new RegraDeNegocioException("Usuário ou senha inválidos.");
        }

        Usuario usuario = usuarioRepository.findByUsuario(usuarioLogin)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário ou senha inválidos."));

        String token = jwtService.gerarToken(usuarioLogin);

        return new LoginResponseDTO(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNome(),
                usuario.getPerfil(),
                usuario.getAdministradora() != null ? usuario.getAdministradora().getId() : null,
                usuario.getCondominio() != null ? usuario.getCondominio().getId() : null
        );
    }
}