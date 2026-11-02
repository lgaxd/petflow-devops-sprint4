package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.SubscriptionRequestDTO;
import br.com.petflow.petflow_api.dto.SubscriptionResponseDTO;
import br.com.petflow.petflow_api.service.SubscriptionService;
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
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Assinaturas", description = "Endpoints para gerenciamento de assinaturas de planos")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Criar assinatura", description = "Cria uma nova assinatura para um pet")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Pet ou plano não encontrado")
    })
    public ResponseEntity<SubscriptionResponseDTO> create(@Valid @RequestBody SubscriptionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar assinaturas", description = "Retorna lista paginada de assinaturas com filtros")
    public ResponseEntity<Page<SubscriptionResponseDTO>> findAll(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "startDate", "endDate", "status", "createdAt"));
        return ResponseEntity.ok(subscriptionService.findAll(petId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar assinatura por ID", description = "Retorna os dados de uma assinatura específica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Assinatura encontrada"),
        @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    public ResponseEntity<SubscriptionResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.findById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status da assinatura", description = "Altera o status da assinatura (ATIVO → ENCERRADO/CANCELADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Assinatura não encontrada"),
        @ApiResponse(responseCode = "422", description = "Transição de status inválida")
    })
    public ResponseEntity<SubscriptionResponseDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(subscriptionService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover assinatura", description = "Remove uma assinatura do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Assinatura removida com sucesso"),
        @ApiResponse(responseCode = "404", description = "Assinatura não encontrada")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}