package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record SubscriptionRequestDTO(
    @NotNull(message = "A data de início é obrigatória")
    LocalDate startDate,

    LocalDate endDate,

    @Size(max = 20, message = "O status deve ter no máximo 20 caracteres")
    String status,

    @NotNull(message = "O ID do pet é obrigatório")
    Long petId,

    @NotNull(message = "O ID do plano é obrigatório")
    Long planId
) {

    public static final String DEFAULT_STATUS = "ATIVO";

    /** Status é opcional no payload: quando ausente (ou em branco) a assinatura nasce ATIVO. */
    public SubscriptionRequestDTO {
        if (status == null || status.isBlank()) {
            status = DEFAULT_STATUS;
        }
    }
}
