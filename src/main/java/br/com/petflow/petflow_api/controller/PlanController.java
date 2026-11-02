package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.PlanRequestDTO;
import br.com.petflow.petflow_api.dto.PlanResponseDTO;
import br.com.petflow.petflow_api.service.PlanService;
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
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Planos", description = "Endpoints para gerenciamento de planos de saúde pet")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @Operation(summary = "Criar novo plano", description = "Cria um novo plano de saúde associado a uma clínica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Plano criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
    })
    public ResponseEntity<PlanResponseDTO> create(@Valid @RequestBody PlanRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar planos", description = "Retorna lista paginada de planos com filtros")
    public ResponseEntity<Page<PlanResponseDTO>> findAll(
            @RequestParam(required = false) Long clinicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "name", "description", "price", "durationDays", "pointsPerEvent"));
        return ResponseEntity.ok(planService.findAll(clinicId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plano por ID", description = "Retorna os dados de um plano específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plano encontrado"),
        @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<PlanResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano", description = "Atualiza os dados de um plano existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Plano não encontrado"),
        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
    })
    public ResponseEntity<PlanResponseDTO> update(@PathVariable Long id, @Valid @RequestBody PlanRequestDTO request) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover plano", description = "Remove um plano do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Plano removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Plano não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}