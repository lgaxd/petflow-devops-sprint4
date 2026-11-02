package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
@Schema(description = "Resposta com dados do plano de saúde pet")
public record PlanResponseDTO(

    @Schema(description = "ID do plano", example = "1")
    Long id,

    @Schema(description = "Nome do plano", example = "Plano Premium")
    String name,

    @Schema(description = "Descrição do plano", example = "Cobertura completa com consultas e exames")
    String description,

    @Schema(description = "Preço mensal", example = "89.90")
    BigDecimal price,

    @Schema(description = "Duração em dias", example = "365")
    Integer durationDays,

    @Schema(description = "Pontos por evento realizado", example = "50")
    Integer pointsPerEvent,

    @Schema(description = "ID da clínica associada", example = "5")
    Long clinicId,

    @Schema(description = "Nome da clínica", example = "PetCare Veterinária")
    String clinicName
) {}