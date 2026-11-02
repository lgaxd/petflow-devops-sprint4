package br.com.petflow.petflow_api.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointHistoryDTO(
    Long id,
    Integer points,
    String reason,
    String referenceType,
    Long referenceId,
    LocalDateTime createdAt
) {}