package br.com.condominio.backend.service;

import br.com.condominio.backend.exception.RegraDeNegocioException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import br.com.condominio.backend.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String autenticar(String usuarioLogin, String senha) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuarioLogin, senha)
            );
        } catch (AuthenticationException excecaoDeAutenticacao) {
            throw new RegraDeNegocioException("Usuário ou senha inválidos.");
        }

        return jwtService.gerarToken(usuarioLogin);
    }
}