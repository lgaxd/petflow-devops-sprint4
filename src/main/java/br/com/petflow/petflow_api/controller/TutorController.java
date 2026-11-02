package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.TutorRequestDTO;
import br.com.petflow.petflow_api.dto.TutorUpdateRequestDTO;
import br.com.petflow.petflow_api.dto.TutorResponseDTO;
import br.com.petflow.petflow_api.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/tutors")
@RequiredArgsConstructor
@Tag(name = "Tutores", description = "Endpoints para gerenciamento de tutores de pets")
public class TutorController {

    private final TutorService tutorService;

    @PostMapping
    @Operation(summary = "Cadastrar novo tutor", description = "Cria um novo tutor no sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tutor criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    public ResponseEntity<TutorResponseDTO> create(@Valid @RequestBody TutorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tutorService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar tutores", description = "Retorna lista paginada de tutores com ordenação")
    public ResponseEntity<Page<TutorResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "name", "email", "createdAt"));
        return ResponseEntity.ok(tutorService.findAll(name, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tutor por ID", description = "Retorna os dados de um tutor específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tutor encontrado"),
        @ApiResponse(responseCode = "404", description = "Tutor não encontrado")
    })
    public ResponseEntity<TutorResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tutorService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tutor", description = "Atualiza os dados de um tutor existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tutor atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tutor não encontrado"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    public ResponseEntity<TutorResponseDTO> update(@PathVariable Long id, @Valid @RequestBody TutorUpdateRequestDTO request) {
        return ResponseEntity.ok(tutorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover tutor", description = "Remove um tutor do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Tutor removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Tutor não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tutorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}