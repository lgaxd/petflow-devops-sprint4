package br.com.petflow.petflow_api.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record PetRequestDTO(
    @NotBlank(message = "O nome do pet é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    String name,

    @Size(max = 50, message = "A raça deve ter no máximo 50 caracteres")
    String breed,

    @Past(message = "A data de nascimento deve ser uma data passada")
    LocalDate birthDate,

    @DecimalMin(value = "0.0", inclusive = false, message = "O peso deve ser positivo")
    @Digits(integer = 3, fraction = 2, message = "Formato de peso inválido")
    BigDecimal weight,

    @NotNull(message = "O ID do tutor é obrigatório")
    Long tutorId,

    @NotNull(message = "O ID da espécie é obrigatório")
    Long speciesId
) {}