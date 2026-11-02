package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.EventTypeResponseDTO;
import br.com.petflow.petflow_api.dto.SpeciesResponseDTO;
import br.com.petflow.petflow_api.service.EventTypeService;
import br.com.petflow.petflow_api.service.SpeciesService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tabelas de domínio expostas para os clientes montarem os formulários (espécies e tipos de evento). */
@WebMvcTest({SpeciesController.class, EventTypeController.class})
class ReferenceDataControllerTest extends ControllerTestSupport {

    @MockitoBean
    private SpeciesService speciesService;

    @MockitoBean
    private EventTypeService eventTypeService;

    @Test
    void speciesReturnsPlainArrayForAnyLoggedUser() throws Exception {
        when(speciesService.findAll()).thenReturn(List.of(
                SpeciesResponseDTO.builder().id(1L).name("Cachorro").description("Canino doméstico").build(),
                SpeciesResponseDTO.builder().id(2L).name("Gato").build()));

        mvc.perform(get("/species").header("Authorization", tutorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Cachorro"))
                .andExpect(jsonPath("$[0].description").value("Canino doméstico"));
        mvc.perform(get("/species").header("Authorization", adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void eventTypesReturnsPlainArrayWithRewardPoints() throws Exception {
        when(eventTypeService.findAll()).thenReturn(List.of(
                EventTypeResponseDTO.builder().id(1L).name("Vacinação").pointsReward(50).category("PREVENTIVO").build()));

        mvc.perform(get("/event-types").header("Authorization", tutorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Vacinação"))
                .andExpect(jsonPath("$[0].pointsReward").value(50))
                .andExpect(jsonPath("$[0].category").value("PREVENTIVO"));
    }

    @Test
    void bothEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/species")).andExpect(status().isUnauthorized());
        mvc.perform(get("/event-types")).andExpect(status().isUnauthorized());
    }

    @Test
    void tablesAreReadOnly() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/species")
                        .header("Authorization", adminToken()))
                .andExpect(status().isMethodNotAllowed());
    }
}
