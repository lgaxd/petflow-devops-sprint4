package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.HealthEventRequestDTO;
import br.com.petflow.petflow_api.dto.HealthEventResponseDTO;
import br.com.petflow.petflow_api.service.HealthEventService;
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
@RequestMapping("/health-events")
@RequiredArgsConstructor
@Tag(name = "Eventos de Saúde", description = "Endpoints para gerenciamento de eventos de saúde dos pets")
public class HealthEventController {

    private final HealthEventService healthEventService;

    @PostMapping
    @Operation(summary = "Registrar evento de saúde",
               description = "Cria um novo evento de saúde para um pet. Eventos AGENDADO ou CANCELADO aceitam data futura; "
                   + "REALIZADO não (422, código FUTURE_DATE_NOT_ALLOWED). Quando marcado como REALIZADO, gera pontos automaticamente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Evento criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Pet ou clínica não encontrado"),
        @ApiResponse(responseCode = "422", description = "Evento REALIZADO com data futura")
    })
    public ResponseEntity<HealthEventResponseDTO> create(@Valid @RequestBody HealthEventRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(healthEventService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar eventos de saúde",
               description = "Retorna lista paginada de eventos. Filtre por petId e/ou status.")
    public ResponseEntity<Page<HealthEventResponseDTO>> findAll(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "description", "eventDate", "status", "createdAt"));
        return ResponseEntity.ok(healthEventService.findAll(petId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por ID", description = "Retorna os dados de um evento específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evento encontrado"),
        @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    public ResponseEntity<HealthEventResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(healthEventService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar evento de saúde",
               description = "Atualiza o evento. Ao mudar status para REALIZADO, o sistema gera pontos de recompensa automaticamente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Evento atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Evento não encontrado"),
        @ApiResponse(responseCode = "422", description = "Transição de status inválida ou evento REALIZADO com data futura")
    })
    public ResponseEntity<HealthEventResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody HealthEventRequestDTO request) {
        return ResponseEntity.ok(healthEventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover evento de saúde", description = "Remove um evento do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Evento removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Evento não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        healthEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}