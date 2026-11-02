package br.com.petflow.petflow_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Espécie de pet (tabela de domínio)")
public record SpeciesResponseDTO(
    @Schema(description = "ID da espécie (use em speciesId ao cadastrar um pet)", example = "1")
    Long id,
    @Schema(description = "Nome da espécie", example = "Cachorro")
    String name,
    @Schema(description = "Descrição da espécie", example = "Canino doméstico")
    String description
) {}
