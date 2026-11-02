package br.com.petflow.petflow_api.dto;

import br.com.petflow.petflow_api.enums.HealthEventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados do evento de saúde do pet")
public record HealthEventResponseDTO(

    @Schema(description = "ID do evento", example = "1")
    Long id,

    @Schema(description = "Descrição do evento", example = "Vacinação contra raiva")
    String description,

    @Schema(description = "Data do evento", example = "2024-03-15")
    LocalDate eventDate,

    @Schema(description = "Status do evento", example = "REALIZADO", allowableValues = {"AGENDADO", "REALIZADO", "CANCELADO"})
    HealthEventStatus status,

    @Schema(description = "Data de registro", example = "2024-01-15T10:30:00")
    LocalDateTime createdAt,

    @Schema(description = "ID do pet", example = "10")
    Long petId,

    @Schema(description = "Nome do pet", example = "Rex")
    String petName,

    @Schema(description = "ID do tipo de evento", example = "1")
    Long eventTypeId,

    @Schema(description = "ID da clínica (opcional)", example = "5")
    Long clinicId,

    @Schema(description = "Nome da clínica", example = "PetCare Veterinária")
    String clinicName
) {}