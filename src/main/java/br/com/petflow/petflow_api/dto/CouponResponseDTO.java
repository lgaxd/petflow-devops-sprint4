package br.com.petflow.petflow_api.dto;

import br.com.petflow.petflow_api.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados do cupom de desconto")
public record CouponResponseDTO(

    @Schema(description = "ID do cupom", example = "1")
    Long id,

    @Schema(description = "Código único do cupom", example = "DESCONTO20")
    String code,

    @Schema(description = "Status do cupom", example = "DISPONIVEL", allowableValues = {"DISPONIVEL", "RESGATADO", "UTILIZADO"})
    CouponStatus status,

    @Schema(description = "Data de expiração", example = "2025-12-31")
    LocalDate expirationDate,

    @Schema(description = "ID do template", example = "1")
    Long templateId,

    @Schema(description = "Data de criação", example = "2024-01-15T10:30:00")
    LocalDateTime createdAt
) {}