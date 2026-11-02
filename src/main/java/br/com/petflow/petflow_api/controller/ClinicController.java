package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.ClinicRequestDTO;
import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.service.ClinicService;
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
@RequestMapping("/clinics")
@RequiredArgsConstructor
@Tag(name = "Clínicas", description = "Endpoints para gerenciamento de clínicas veterinárias")
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    @Operation(summary = "Cadastrar nova clínica", description = "Cria uma nova clínica veterinária no sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Clínica criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado")
    })
    public ResponseEntity<ClinicResponseDTO> create(@Valid @RequestBody ClinicRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar clínicas", description = "Retorna lista paginada de clínicas com filtros e ordenação")
    public ResponseEntity<Page<ClinicResponseDTO>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "name", "address", "phone", "cnpj", "createdAt"));
        return ResponseEntity.ok(clinicService.findAll(name, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar clínica por ID", description = "Retorna os dados de uma clínica específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clínica encontrada"),
        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
    })
    public ResponseEntity<ClinicResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(clinicService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar clínica", description = "Atualiza os dados de uma clínica existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clínica atualizada com sucesso"),
        @ApiResponse(responseCode = "404", description = "Clínica não encontrada"),
        @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado")
    })
    public ResponseEntity<ClinicResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ClinicRequestDTO request) {
        return ResponseEntity.ok(clinicService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover clínica", description = "Remove uma clínica do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Clínica removida com sucesso"),
        @ApiResponse(responseCode = "404", description = "Clínica não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clinicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}