package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados do tutor")
public record TutorResponseDTO(

    @Schema(description = "ID do tutor", example = "1")
    Long id,

    @Schema(description = "Nome completo", example = "João Silva")
    String name,

    @Schema(description = "E-mail do tutor", example = "joao.silva@email.com")
    String email,

    @Schema(description = "Telefone para contato", example = "(11) 99999-9999")
    String phone,

    @Schema(description = "Data de cadastro", example = "2024-01-15T10:30:00")
    LocalDateTime createdAt
) {}