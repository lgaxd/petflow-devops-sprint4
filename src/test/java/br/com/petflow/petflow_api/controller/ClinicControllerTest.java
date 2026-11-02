package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.service.ClinicService;
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

@WebMvcTest(ClinicController.class)
class ClinicControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"name":"PetCare","address":"Rua 1","phone":"1199999","cnpj":"12.345.678/0001-90"}""";

    @MockitoBean
    private ClinicService clinicService;

    private final ClinicResponseDTO clinic = ClinicResponseDTO.builder().id(1L).name("PetCare").build();

    @Test
    void createReturns201ForAdmin() throws Exception {
        when(clinicService.create(any())).thenReturn(clinic);

        mvc.perform(json(post("/clinics"), adminToken(), BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("PetCare"));
    }

    @Test
    void createIsForbiddenForTutorAndUnauthorizedWithoutToken() throws Exception {
        mvc.perform(json(post("/clinics"), tutorToken(), BODY)).andExpect(status().isForbidden());
        mvc.perform(post("/clinics").contentType("application/json").content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRejectsInvalidBodyWithFieldErrors() throws Exception {
        mvc.perform(json(post("/clinics"), adminToken(), "{\"name\":\"\",\"cnpj\":\"1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.cnpj").exists());
    }

    @Test
    void listIsPagedAndAvailableToTutor() throws Exception {
        when(clinicService.findAll(any(), any())).thenReturn(page(clinic));

        mvc.perform(get("/clinics").header("Authorization", tutorToken()).param("name", "Pet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("PetCare"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listRejectsUnknownSortField() throws Exception {
        mvc.perform(get("/clinics").header("Authorization", tutorToken()).param("sortBy", "password"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByIdReturnsClinicOr404() throws Exception {
        when(clinicService.findById(1L)).thenReturn(clinic);
        when(clinicService.findById(99L)).thenThrow(new EntityNotFoundException("Clínica", 99L));

        mvc.perform(get("/clinics/1").header("Authorization", tutorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/clinics/99").header("Authorization", tutorToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAndDeleteRequireAdmin() throws Exception {
        when(clinicService.update(eq(1L), any())).thenReturn(clinic);

        mvc.perform(json(put("/clinics/1"), adminToken(), BODY)).andExpect(status().isOk());
        mvc.perform(json(put("/clinics/1"), tutorToken(), BODY)).andExpect(status().isForbidden());
        mvc.perform(delete("/clinics/1").header("Authorization", adminToken())).andExpect(status().isNoContent());
        mvc.perform(delete("/clinics/1").header("Authorization", tutorToken())).andExpect(status().isForbidden());
        verify(clinicService).delete(1L);
    }
}
