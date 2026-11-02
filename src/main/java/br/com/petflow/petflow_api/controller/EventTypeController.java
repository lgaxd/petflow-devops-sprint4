package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.EventTypeResponseDTO;
import br.com.petflow.petflow_api.service.EventTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event-types")
@RequiredArgsConstructor
@Tag(name = "Tipos de evento", description = "Tabela de domínio com os tipos de evento de saúde")
public class EventTypeController {

    private final EventTypeService eventTypeService;

    @GetMapping
    @Operation(summary = "Listar tipos de evento",
            description = "Retorna todos os tipos de evento de saúde, ordenados por ID, sem paginação "
                    + "(tabela de domínio pequena). Os IDs são usados em eventTypeId ao registrar um evento.")
    public ResponseEntity<List<EventTypeResponseDTO>> findAll() {
        return ResponseEntity.ok(eventTypeService.findAll());
    }
}
