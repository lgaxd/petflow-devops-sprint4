package br.com.petflow.petflow_api.dto;

import lombok.Builder;

@Builder
public record PetRiskDTO(
    Long petId,
    String petName,
    Integer score,
    String riskLevel,
    String riskDescription
) {}