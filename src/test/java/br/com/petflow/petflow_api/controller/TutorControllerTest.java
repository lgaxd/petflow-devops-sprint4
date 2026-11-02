package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.TutorResponseDTO;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.service.TutorService;
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

@WebMvcTest(TutorController.class)
class TutorControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"name":"Maria","email":"maria@petflow.com","phone":"11999","password":"senha1234"}""";

    @MockitoBean
    private TutorService tutorService;

    private final TutorResponseDTO tutor = TutorResponseDTO.builder().id(5L).name("Maria").email("maria@petflow.com").build();

    @Test
    void registrationIsPublic() throws Exception {
        when(tutorService.create(any())).thenReturn(tutor);

        mvc.perform(post("/tutors").contentType("application/json").content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@petflow.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationRejectsWeakPasswordAndDuplicateEmail() throws Exception {
        mvc.perform(post("/tutors").contentType("application/json")
                        .content("{\"name\":\"M\",\"email\":\"m@m.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        when(tutorService.create(any())).thenThrow(new DuplicateResourceException("Tutor", "email", "maria@petflow.com"));
        mvc.perform(post("/tutors").contentType("application/json").content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void managementEndpointsAreAdminOnly() throws Exception {
        when(tutorService.findAll(any(), any())).thenReturn(page(tutor));
        when(tutorService.findById(5L)).thenReturn(tutor);
        when(tutorService.update(eq(5L), any())).thenReturn(tutor);

        mvc.perform(get("/tutors").header("Authorization", adminToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(5));
        mvc.perform(get("/tutors/5").header("Authorization", adminToken())).andExpect(status().isOk());
        mvc.perform(json(put("/tutors/5"), adminToken(), BODY)).andExpect(status().isOk());
        mvc.perform(delete("/tutors/5").header("Authorization", adminToken())).andExpect(status().isNoContent());
        verify(tutorService).delete(5L);

        mvc.perform(get("/tutors").header("Authorization", tutorToken())).andExpect(status().isForbidden());
        mvc.perform(get("/tutors/5").header("Authorization", tutorToken())).andExpect(status().isForbidden());
        mvc.perform(json(put("/tutors/5"), tutorToken(), BODY)).andExpect(status().isForbidden());
        mvc.perform(delete("/tutors/5").header("Authorization", tutorToken())).andExpect(status().isForbidden());
        mvc.perform(get("/tutors")).andExpect(status().isUnauthorized());
    }

    @Test
    void updateAcceptsMissingPasswordButRejectsWeakOne() throws Exception {
        when(tutorService.update(eq(5L), any())).thenReturn(tutor);

        mvc.perform(json(put("/tutors/5"), adminToken(), "{\"name\":\"Maria\",\"email\":\"maria@petflow.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/tutors/5"), adminToken(), "{\"name\":\"Maria\",\"email\":\"maria@petflow.com\",\"password\":\"\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/tutors/5"), adminToken(), "{\"name\":\"Maria\",\"email\":\"maria@petflow.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(tutorService.findById(99L)).thenThrow(new EntityNotFoundException("Tutor", 99L));

        mvc.perform(get("/tutors/99").header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/tutors/99"));
    }

    @Test
    void invalidPathVariableReturns400() throws Exception {
        mvc.perform(get("/tutors/abc").header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidPagingReturns400() throws Exception {
        mvc.perform(get("/tutors").header("Authorization", adminToken()).param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/tutors").header("Authorization", adminToken()).param("direction", "sideways"))
                .andExpect(status().isBadRequest());
    }
}
