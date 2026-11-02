package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record RedeemRequestDTO(
    @NotNull(message = "Os pontos utilizados são obrigatórios")
    @Positive(message = "Os pontos utilizados devem ser positivos")
    Integer pointsUsed,

    @NotNull(message = "O ID do tutor é obrigatório")
    Long tutorId,

    @NotNull(message = "O ID do cupom é obrigatório")
    Long couponId
) {}