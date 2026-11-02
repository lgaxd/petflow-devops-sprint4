package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.dto.LoginRequestDTO;
import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.dto.SubscriptionRequestDTO;
import br.com.petflow.petflow_api.dto.TutorRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoRecordContractTest {

        private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void recordSerializationKeepsExistingJsonPropertyNames() throws Exception {
        PetResponseDTO response = new PetResponseDTO(
                7L,
                "Milo",
                "Poodle",
                LocalDate.of(2021, 5, 15),
                new BigDecimal("4.80"),
                2L,
                LocalDateTime.of(2026, 1, 2, 3, 4),
                1L,
                "Tutor A");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));

        assertEquals(7L, json.get("id").asLong());
        assertEquals("Milo", json.get("name").asText());
        assertEquals("2021-05-15", json.get("birthDate").asText());
        assertEquals(1L, json.get("tutorId").asLong());
        assertTrue(json.has("createdAt"));
        assertFalse(json.has("tutor_id"));
    }

    @Test
    void recordDeserializationAndDefaultsRemainCompatible() throws Exception {
        LoginRequestDTO login = objectMapper.readValue(
                "{\"email\":\"tutor@example.com\",\"password\":\"safe1234\"}",
                LoginRequestDTO.class);
        SubscriptionRequestDTO subscription = objectMapper.readValue(
                "{\"startDate\":\"2026-01-01\",\"petId\":1,\"planId\":2}",
                SubscriptionRequestDTO.class);

        assertEquals("tutor@example.com", login.email());
        assertEquals("safe1234", login.password());
        assertEquals("ATIVO", subscription.status());
        assertEquals("ATIVO", SubscriptionRequestDTO.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .petId(1L)
                .planId(2L)
                .build()
                .status());
    }

    @Test
    void missingOrBlankSubscriptionStatusDefaultsToAtivo() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        SubscriptionRequestDTO subscription = new SubscriptionRequestDTO(LocalDate.of(2026, 1, 1), null, null, 1L, 2L);

        assertEquals("ATIVO", subscription.status());
        assertEquals("ATIVO", new SubscriptionRequestDTO(LocalDate.of(2026, 1, 1), null, "  ", 1L, 2L).status());
        assertTrue(validator.validate(subscription).isEmpty());
    }

    @Test
    void recordComponentsRetainBeanValidationConstraints() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        TutorRequestDTO request = new TutorRequestDTO("", "invalid-email", null, "short");

        assertFalse(validator.validate(request).isEmpty());
    }
}