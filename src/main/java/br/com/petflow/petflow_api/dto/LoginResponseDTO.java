package br.com.petflow.petflow_api.dto;

import br.com.petflow.petflow_api.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "Token JWT e dados do usuário autenticado")
public record LoginResponseDTO(
    @Schema(description = "Token JWT (enviar em Authorization: Bearer <token>)")
    String token,
    @Schema(description = "ID do usuário", example = "5")
    Long id,
    @Schema(description = "Nome do usuário", example = "Maria Silva")
    String name,
    @Schema(description = "E-mail do usuário", example = "maria@petflow.com")
    String email,
    @Schema(description = "Perfil do usuário", example = "TUTOR")
    UserRole role,
    @Schema(description = "Instante (UTC) em que o token expira; use para agendar a renovação em /auth/refresh",
            example = "2026-11-02T12:00:00Z")
    Instant expiresAt
) {}
