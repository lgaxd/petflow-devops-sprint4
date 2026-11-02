package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.HealthEventResponseDTO;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.exception.InvalidStatusTransitionException;
import br.com.petflow.petflow_api.service.HealthEventService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthEventController.class)
class HealthEventControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"description":"Vacina","eventDate":"2024-01-10","status":"AGENDADO","petId":10,"eventTypeId":1,"clinicId":2}""";

    @MockitoBean
    private HealthEventService healthEventService;

    private final HealthEventResponseDTO event = HealthEventResponseDTO.builder()
            .id(20L).status(HealthEventStatus.AGENDADO).petId(10L).build();

    @Test
    void createReturns201() throws Exception {
        when(healthEventService.create(any())).thenReturn(event);

        mvc.perform(json(post("/health-events"), tutorToken(), BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGENDADO"));
    }

    @Test
    void createAcceptsFutureDateForScheduledEvent() throws Exception {
        when(healthEventService.create(any())).thenReturn(event);

        mvc.perform(json(post("/health-events"), tutorToken(),
                        BODY.replace("2024-01-10", "2999-01-01")))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturns422WhenServiceRejectsFutureRealizedEvent() throws Exception {
        when(healthEventService.create(any())).thenThrow(new BusinessRuleException(
                "Um evento realizado não pode ter data futura", "FUTURE_DATE_NOT_ALLOWED"));

        mvc.perform(json(post("/health-events"), tutorToken(),
                        BODY.replace("2024-01-10", "2999-01-01").replace("AGENDADO", "REALIZADO")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("FUTURE_DATE_NOT_ALLOWED"));
    }

    @Test
    void createRejectsMissingIds() throws Exception {
        mvc.perform(json(post("/health-events"), tutorToken(),
                        "{\"eventDate\":\"2999-01-01\",\"status\":\"AGENDADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.eventDate").doesNotExist())
                .andExpect(jsonPath("$.validationErrors.petId").exists())
                .andExpect(jsonPath("$.validationErrors.eventTypeId").exists());
        mvc.perform(post("/health-events").contentType("application/json").content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listFiltersByPetAndStatus() throws Exception {
        when(healthEventService.findAll(eq(10L), eq("AGENDADO"), any())).thenReturn(page(event));

        mvc.perform(get("/health-events").header("Authorization", tutorToken())
                        .param("petId", "10").param("status", "AGENDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(20));
    }

    @Test
    void findByIdAndNotFound() throws Exception {
        when(healthEventService.findById(20L)).thenReturn(event);
        when(healthEventService.findById(404L)).thenThrow(new EntityNotFoundException("Evento de Saúde", 404L));

        mvc.perform(get("/health-events/20").header("Authorization", tutorToken())).andExpect(status().isOk());
        mvc.perform(get("/health-events/404").header("Authorization", tutorToken())).andExpect(status().isNotFound());
    }

    @Test
    void updateReturns422OnInvalidStatusTransition() throws Exception {
        when(healthEventService.update(eq(20L), any())).thenReturn(event);
        when(healthEventService.update(eq(21L), any()))
                .thenThrow(new InvalidStatusTransitionException("HealthEvent", "REALIZADO", "AGENDADO"));

        mvc.perform(json(put("/health-events/20"), tutorToken(), BODY)).andExpect(status().isOk());
        mvc.perform(json(put("/health-events/21"), tutorToken(), BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details.currentStatus").value("REALIZADO"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/health-events/20").header("Authorization", tutorToken()))
                .andExpect(status().isNoContent());
        verify(healthEventService).delete(20L);
    }
}
