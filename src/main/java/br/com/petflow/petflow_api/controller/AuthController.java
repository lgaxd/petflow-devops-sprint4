package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.LoginRequestDTO;
import br.com.petflow.petflow_api.dto.LoginResponseDTO;
import br.com.petflow.petflow_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login, renovação e emissão de token JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica um tutor ou admin e retorna um token JWT")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token",
            description = "Troca um token JWT ainda válido por um novo, relendo os dados do usuário. "
                    + "Use o campo expiresAt do login para renovar antes de expirar. "
                    + "A sessão tem um tempo máximo; depois dele é preciso fazer login novamente.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Novo token emitido"),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado, usuário removido "
                + "(code TOKEN_INVALID) ou sessão no tempo máximo (code SESSION_EXPIRED)")
    })
    public ResponseEntity<LoginResponseDTO> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(authService.refresh(authorization));
    }
}
