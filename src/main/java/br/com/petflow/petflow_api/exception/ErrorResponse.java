package br.com.petflow.petflow_api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    LocalDateTime timestamp,
    Integer status,
    String error,
    String message,
    String code,
    String path,
    Map<String, String> validationErrors,
    Map<String, Object> details
) {}