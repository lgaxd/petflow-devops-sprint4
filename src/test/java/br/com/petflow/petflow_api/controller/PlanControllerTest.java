package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.PlanResponseDTO;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanController.class)
class PlanControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"name":"Premium","description":"Completo","price":89.90,"durationDays":365,"pointsPerEvent":5,"clinicId":1}""";

    @MockitoBean
    private PlanService planService;

    private final PlanResponseDTO plan = PlanResponseDTO.builder()
            .id(2L).name("Premium").price(new BigDecimal("89.90")).clinicId(1L).build();

    @Test
    void createReturns201ForAdminAnd403ForTutor() throws Exception {
        when(planService.create(any())).thenReturn(plan);

        mvc.perform(json(post("/plans"), adminToken(), BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2));
        mvc.perform(json(post("/plans"), tutorToken(), BODY)).andExpect(status().isForbidden());
    }

    @Test
    void createValidatesRequiredFields() throws Exception {
        mvc.perform(json(post("/plans"), adminToken(), "{\"name\":\"x\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.clinicId").exists());
    }

    @Test
    void listFiltersByClinic() throws Exception {
        when(planService.findAll(eq(1L), any())).thenReturn(page(plan));

        mvc.perform(get("/plans").header("Authorization", tutorToken()).param("clinicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Premium"));
    }

    @Test
    void findByIdUpdateAndDelete() throws Exception {
        when(planService.findById(2L)).thenReturn(plan);
        when(planService.findById(9L)).thenThrow(new EntityNotFoundException("Plano", 9L));
        when(planService.update(eq(2L), any())).thenReturn(plan);

        mvc.perform(get("/plans/2").header("Authorization", tutorToken())).andExpect(status().isOk());
        mvc.perform(get("/plans/9").header("Authorization", tutorToken())).andExpect(status().isNotFound());
        mvc.perform(json(put("/plans/2"), adminToken(), BODY)).andExpect(status().isOk());
        mvc.perform(delete("/plans/2").header("Authorization", adminToken())).andExpect(status().isNoContent());
        mvc.perform(delete("/plans/2").header("Authorization", tutorToken())).andExpect(status().isForbidden());
    }
}
