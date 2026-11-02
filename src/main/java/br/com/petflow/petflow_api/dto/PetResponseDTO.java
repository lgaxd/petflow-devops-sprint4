package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Resposta com dados do pet")
public record PetResponseDTO(

    @Schema(description = "ID do pet", example = "1")
    Long id,

    @Schema(description = "Nome do pet", example = "Rex")
    String name,

    @Schema(description = "Raça do pet", example = "Labrador Retriever")
    String breed,

    @Schema(description = "Data de nascimento", example = "2020-05-20")
    LocalDate birthDate,

    @Schema(description = "Peso em kg", example = "25.5")
    BigDecimal weight,

    @Schema(description = "ID da espécie", example = "1")
    Long speciesId,

    @Schema(description = "Data de cadastro", example = "2024-01-15T10:30:00")
    LocalDateTime createdAt,

    @Schema(description = "ID do tutor responsável", example = "10")
    Long tutorId,

    @Schema(description = "Nome do tutor", example = "João Silva")
    String tutorName
) {}