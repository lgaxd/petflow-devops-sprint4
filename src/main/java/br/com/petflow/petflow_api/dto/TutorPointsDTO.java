package br.com.petflow.petflow_api.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record TutorPointsDTO(
    Long tutorId,
    String tutorName,
    Integer totalPoints,
    List<PointHistoryDTO> history
) {}