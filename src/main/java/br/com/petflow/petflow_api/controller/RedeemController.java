package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.RedeemResponseDTO;
import br.com.petflow.petflow_api.service.RedeemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/redeems")
@RequiredArgsConstructor
@Tag(name = "Resgates", description = "Endpoints para gerenciamento de resgates de cupons")
public class RedeemController {

    private final RedeemService redeemService;

    @GetMapping
    @Operation(summary = "Listar resgates", description = "Retorna lista paginada de resgates com filtros")
    public ResponseEntity<Page<RedeemResponseDTO>> findAll(
            @RequestParam(required = false) Long tutorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "pointsUsed", "createdAt"));
        return ResponseEntity.ok(redeemService.findAll(tutorId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar resgate por ID", description = "Retorna os dados de um resgate específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resgate encontrado"),
        @ApiResponse(responseCode = "404", description = "Resgate não encontrado")
    })
    public ResponseEntity<RedeemResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(redeemService.findById(id));
    }
}