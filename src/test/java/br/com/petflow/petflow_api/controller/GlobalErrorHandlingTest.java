package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.service.ClinicService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante um envelope JSON consistente para erros que não pertencem a um controller específico. */
@WebMvcTest(ClinicController.class)
class GlobalErrorHandlingTest extends ControllerTestSupport {

    @MockitoBean
    private ClinicService clinicService;

    @Test
    void unknownRouteReturns404Json() throws Exception {
        mvc.perform(get("/rota-inexistente").header("Authorization", adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Endpoint não encontrado"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(patch("/clinics/1").header("Authorization", adminToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(json(post("/clinics"), adminToken(), "{nao-e-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("JSON inválido"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mvc.perform(post("/clinics").header("Authorization", adminToken())
                        .contentType("text/plain").content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void foreignKeyViolationOnDeleteReturns409WithFriendlyMessage() throws Exception {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("erro",
                        new SQLException("ORA-02292: integrity constraint violated - child record found")))
                .when(clinicService).delete(1L);

        mvc.perform(delete("/clinics/1").header("Authorization", adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O registro possui dependências e não pode ser removido."));
    }

    @Test
    void unexpectedErrorsReturn500WithoutLeakingDetails() throws Exception {
        when(clinicService.findAll(any(), any())).thenThrow(new IllegalStateException("segredo interno"));

        mvc.perform(get("/clinics").header("Authorization", adminToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocorreu um erro inesperado."));
    }
}
