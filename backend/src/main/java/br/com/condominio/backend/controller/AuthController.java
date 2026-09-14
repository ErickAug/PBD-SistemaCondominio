package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.LoginRequestDTO;
import br.com.condominio.backend.dto.LoginResponseDTO;
import br.com.condominio.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO dto) {
        LoginResponseDTO resposta = authService.autenticar(dto.usuario(), dto.senha());
        return ResponseEntity.ok(resposta);
    }
}