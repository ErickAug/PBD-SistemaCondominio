package br.com.condominio.backend.controller;

import br.com.condominio.backend.dto.AdministradoraRequestDTO;
import br.com.condominio.backend.dto.AdministradoraResponseDTO;
import br.com.condominio.backend.model.Administradora;
import br.com.condominio.backend.service.AdministradoraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administradoras")
public class AdministradoraController {

    private final AdministradoraService administradoraService;

    public AdministradoraController(AdministradoraService administradoraService) {
        this.administradoraService = administradoraService;
    }

    @PostMapping
    public ResponseEntity<AdministradoraResponseDTO> cadastrar(@RequestBody AdministradoraRequestDTO dto) {
        Administradora administradora = new Administradora();
        administradora.setNome(dto.nome());
        administradora.setCnpj(dto.cnpj());

        Administradora salva = administradoraService.cadastrar(administradora);

        AdministradoraResponseDTO resposta =
                new AdministradoraResponseDTO(salva.getId(), salva.getNome(), salva.getCnpj());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}