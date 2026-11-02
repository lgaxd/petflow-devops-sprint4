package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record TutorRequestDTO(
    @NotBlank(message = "O nome do tutor é obrigatório")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
    String name,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres")
    String email,

    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    String phone,

    @NotBlank(message = "A senha é obrigatória")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", 
         message = "A senha deve ter no mínimo 8 caracteres, contendo pelo menos uma letra e um número")
    String password
) {}