package br.com.petflow.petflow_api.dto;

import br.com.petflow.petflow_api.enums.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados da assinatura do pet")
public record SubscriptionResponseDTO(

    @Schema(description = "ID da assinatura", example = "1")
    Long id,

    @Schema(description = "Data de início", example = "2024-01-01")
    LocalDate startDate,

    @Schema(description = "Data de término", example = "2025-01-01")
    LocalDate endDate,

    @Schema(description = "Status da assinatura", example = "ATIVO", allowableValues = {"ATIVO", "ENCERRADO", "CANCELADO", "EXPIRADO"})
    SubscriptionStatus status,

    @Schema(description = "Data de criação", example = "2024-01-01T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "ID do pet", example = "10")
    Long petId,

    @Schema(description = "Nome do pet", example = "Rex")
    String petName,

    @Schema(description = "ID do plano", example = "5")
    Long planId,

    @Schema(description = "Nome do plano", example = "Plano Premium")
    String planName
) {}