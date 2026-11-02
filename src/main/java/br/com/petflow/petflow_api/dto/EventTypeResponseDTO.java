package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Tipo de evento de saúde (tabela de domínio)")
public record EventTypeResponseDTO(
    @Schema(description = "ID do tipo (use em eventTypeId ao registrar um evento)", example = "1")
    Long id,
    @Schema(description = "Nome do tipo de evento", example = "Vacinação")
    String name,
    @Schema(description = "Pontos concedidos ao tutor quando o evento é realizado", example = "50")
    Integer pointsReward,
    @Schema(description = "Categoria do evento", example = "PREVENTIVO")
    String category
) {}
