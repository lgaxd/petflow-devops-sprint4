package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.time.LocalDate;

@Builder
public record HealthEventRequestDTO(
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
    String description,

    @NotNull(message = "A data do evento é obrigatória")
    LocalDate eventDate,

    @NotBlank(message = "O status é obrigatório")
    @Size(max = 20, message = "O status deve ter no máximo 20 caracteres")
    String status,

    @NotNull(message = "O ID do pet é obrigatório")
    Long petId,

    @NotNull(message = "O ID do tipo de evento é obrigatório")
    Long eventTypeId,

    Long clinicId
) {}