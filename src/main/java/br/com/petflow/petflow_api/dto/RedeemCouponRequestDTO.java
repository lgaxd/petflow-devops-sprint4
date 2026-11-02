package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RedeemCouponRequestDTO(
    @NotNull(message = "O ID do cupom é obrigatório")
    Long couponId
) {}