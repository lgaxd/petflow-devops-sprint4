package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.SpeciesResponseDTO;
import br.com.petflow.petflow_api.service.SpeciesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/species")
@RequiredArgsConstructor
@Tag(name = "Espécies", description = "Tabela de domínio com as espécies de pet")
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping
    @Operation(summary = "Listar espécies",
            description = "Retorna todas as espécies, ordenadas por ID, sem paginação (tabela de domínio pequena). "
                    + "Os IDs são usados em speciesId ao cadastrar um pet.")
    public ResponseEntity<List<SpeciesResponseDTO>> findAll() {
        return ResponseEntity.ok(speciesService.findAll());
    }
}
