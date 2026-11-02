package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.SubscriptionResponseDTO;
import br.com.petflow.petflow_api.enums.SubscriptionStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.exception.InvalidStatusTransitionException;
import br.com.petflow.petflow_api.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.com.petflow.petflow_api.dto.SubscriptionRequestDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest extends ControllerTestSupport {

    @MockitoBean
    private SubscriptionService subscriptionService;

    private final SubscriptionResponseDTO subscription = SubscriptionResponseDTO.builder()
            .id(30L).status(SubscriptionStatus.ATIVO).petId(10L).planId(2L).build();

    @Test
    void createDefaultsStatusToAtivoWhenOmitted() throws Exception {
        when(subscriptionService.create(any())).thenReturn(subscription);

        mvc.perform(json(post("/subscriptions"), tutorToken(),
                        "{\"startDate\":\"2024-01-01\",\"petId\":10,\"planId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        ArgumentCaptor<SubscriptionRequestDTO> captor = ArgumentCaptor.forClass(SubscriptionRequestDTO.class);
        verify(subscriptionService).create(captor.capture());
        assertEquals("ATIVO", captor.getValue().status());
    }

    @Test
    void createRejectsMissingFieldsAndAnonymousCalls() throws Exception {
        mvc.perform(json(post("/subscriptions"), tutorToken(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.startDate").exists())
                .andExpect(jsonPath("$.validationErrors.petId").exists())
                .andExpect(jsonPath("$.validationErrors.planId").exists());
        mvc.perform(post("/subscriptions").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMapsInvalidDateRangeTo422() throws Exception {
        when(subscriptionService.create(any()))
                .thenThrow(new BusinessRuleException("fim antes do início", "INVALID_DATE_RANGE"));

        mvc.perform(json(post("/subscriptions"), tutorToken(),
                        "{\"startDate\":\"2024-02-01\",\"endDate\":\"2024-01-01\",\"petId\":10,\"planId\":2}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    void listFiltersByPetAndStatus() throws Exception {
        when(subscriptionService.findAll(eq(10L), eq("ATIVO"), any())).thenReturn(page(subscription));

        mvc.perform(get("/subscriptions").header("Authorization", tutorToken())
                        .param("petId", "10").param("status", "ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].planId").value(2));
    }

    @Test
    void findByIdAndNotFound() throws Exception {
        when(subscriptionService.findById(30L)).thenReturn(subscription);
        when(subscriptionService.findById(404L)).thenThrow(new EntityNotFoundException("Assinatura", 404L));

        mvc.perform(get("/subscriptions/30").header("Authorization", tutorToken())).andExpect(status().isOk());
        mvc.perform(get("/subscriptions/404").header("Authorization", tutorToken())).andExpect(status().isNotFound());
    }

    @Test
    void updateStatusAndInvalidTransition() throws Exception {
        when(subscriptionService.updateStatus(30L, "CANCELADO")).thenReturn(subscription);
        when(subscriptionService.updateStatus(31L, "ATIVO"))
                .thenThrow(new InvalidStatusTransitionException("Subscription", "CANCELADO", "ATIVO"));

        mvc.perform(put("/subscriptions/30/status").header("Authorization", tutorToken()).param("status", "CANCELADO"))
                .andExpect(status().isOk());
        mvc.perform(put("/subscriptions/31/status").header("Authorization", tutorToken()).param("status", "ATIVO"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(put("/subscriptions/30/status").header("Authorization", tutorToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/subscriptions/30").header("Authorization", tutorToken()))
                .andExpect(status().isNoContent());
        verify(subscriptionService).delete(30L);
    }
}
