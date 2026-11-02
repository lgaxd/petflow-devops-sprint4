package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados do resgate de cupom")
public record RedeemResponseDTO(

    @Schema(description = "ID do resgate", example = "1")
    Long id,

    @Schema(description = "Pontos utilizados no resgate", example = "100")
    Integer pointsUsed,

    @Schema(description = "Data do resgate", example = "2024-06-15T14:30:00")
    LocalDateTime createdAt,

    @Schema(description = "ID do tutor", example = "10")
    Long tutorId,

    @Schema(description = "Nome do tutor", example = "João Silva")
    String tutorName,

    @Schema(description = "ID do cupom resgatado", example = "20")
    Long couponId,

    @Schema(description = "Código do cupom", example = "DESCONTO20")
    String couponCode
) {}