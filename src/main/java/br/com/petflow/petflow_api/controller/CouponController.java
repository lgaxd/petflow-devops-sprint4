package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.CouponRequestDTO;
import br.com.petflow.petflow_api.dto.CouponResponseDTO;
import br.com.petflow.petflow_api.service.CouponService;
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
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Cupons", description = "Endpoints para gerenciamento de cupons de desconto")
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @Operation(summary = "Gerar novo cupom", description = "Cria um novo cupom de desconto")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cupom criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "Código do cupom já existe")
    })
    public ResponseEntity<CouponResponseDTO> create(@Valid @RequestBody CouponRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar cupons", description = "Retorna lista paginada de cupons com filtros")
    public ResponseEntity<Page<CouponResponseDTO>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = ApiPageable.of(page, size, sortBy, direction,
            Set.of("id", "code", "status", "expirationDate", "createdAt"));
        return ResponseEntity.ok(couponService.findAll(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cupom por ID", description = "Retorna os dados de um cupom específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cupom encontrado"),
        @ApiResponse(responseCode = "404", description = "Cupom não encontrado")
    })
    public ResponseEntity<CouponResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(couponService.findById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status do cupom", description = "Altera o status do cupom (DISPONIVEL → UTILIZADO ou RESGATADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cupom não encontrado"),
        @ApiResponse(responseCode = "422", description = "Transição de status inválida")
    })
    public ResponseEntity<CouponResponseDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(couponService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover cupom", description = "Remove um cupom do sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cupom removido com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cupom não encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}