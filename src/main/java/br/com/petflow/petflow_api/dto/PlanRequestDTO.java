package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record PlanRequestDTO(
    @NotBlank(message = "O nome do plano é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String name,

    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
    String description,

    @NotNull(message = "O preço é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "O preço deve ser positivo")
    @Digits(integer = 8, fraction = 2, message = "Formato de preço inválido (máximo 10 dígitos, 2 decimais)")
    BigDecimal price,

    @NotNull(message = "A duração em dias é obrigatória")
    @Positive(message = "A duração deve ser positiva")
    Integer durationDays,

    @NotNull(message = "Os pontos por evento são obrigatórios")
    @PositiveOrZero(message = "Os pontos por evento não podem ser negativos")
    Integer pointsPerEvent,

    @NotNull(message = "O ID da clínica é obrigatório")
    Long clinicId
) {}