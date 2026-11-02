package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
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

@WebMvcTest(PetController.class)
class PetControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"name":"Rex","breed":"Labrador","birthDate":"2020-05-20","weight":25.5,"tutorId":7,"speciesId":1}""";

    @MockitoBean
    private PetService petService;

    private final PetResponseDTO pet = PetResponseDTO.builder().id(10L).name("Rex").tutorId(TUTOR_ID).build();

    @Test
    void createReturns201() throws Exception {
        when(petService.create(any())).thenReturn(pet);

        mvc.perform(json(post("/pets"), tutorToken(), BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rex"));
    }

    @Test
    void createRequiresAuthenticationAndValidBody() throws Exception {
        mvc.perform(post("/pets").contentType("application/json").content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/pets"), tutorToken(), "{\"name\":\"\",\"weight\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.weight").exists())
                .andExpect(jsonPath("$.validationErrors.tutorId").exists());
    }

    @Test
    void createForAnotherTutorIsForbidden() throws Exception {
        when(petService.create(any())).thenThrow(new AccessDeniedException("Você não pode cadastrar pets para outro tutor."));

        mvc.perform(json(post("/pets"), tutorToken(), BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não pode cadastrar pets para outro tutor."));
    }

    @Test
    void listPassesFiltersAndPaging() throws Exception {
        when(petService.findAll(eq("Rex"), eq(7L), any())).thenReturn(page(pet));

        mvc.perform(get("/pets").header("Authorization", tutorToken())
                        .param("name", "Rex").param("tutorId", "7").param("sortBy", "birthDate").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void findByIdUpdateAndDelete() throws Exception {
        when(petService.findById(10L)).thenReturn(pet);
        when(petService.findById(404L)).thenThrow(new EntityNotFoundException("Pet", 404L));
        when(petService.update(eq(10L), any())).thenReturn(pet);

        mvc.perform(get("/pets/10").header("Authorization", tutorToken())).andExpect(status().isOk());
        mvc.perform(get("/pets/404").header("Authorization", tutorToken())).andExpect(status().isNotFound());
        mvc.perform(json(put("/pets/10"), tutorToken(), BODY)).andExpect(status().isOk());
        mvc.perform(delete("/pets/10").header("Authorization", tutorToken())).andExpect(status().isNoContent());
        verify(petService).delete(10L);
    }
}
