package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.security.CustomUserDetailsService;
import br.com.petflow.petflow_api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({CustomUserDetailsService.class, AuthService.class})
class AuthControllerTest extends ControllerTestSupport {

    @MockitoBean
    private TutorRepository tutorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tutor registerTutor(UserRole role) {
        Tutor tutor = Tutor.builder()
                .id(5L).name("Maria").email("maria@petflow.com")
                .passwordHash(passwordEncoder.encode("senha1234")).role(role).build();
        when(tutorRepository.findByEmail("maria@petflow.com")).thenReturn(Optional.of(tutor));
        when(tutorRepository.findById(5L)).thenReturn(Optional.of(tutor));
        return tutor;
    }

    private String loginToken() throws Exception {
        String response = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@petflow.com\",\"password\":\"senha1234\"}"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.token");
    }

    @Test
    void loginReturnsExpirationInstant() throws Exception {
        registerTutor(UserRole.TUTOR);

        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@petflow.com\",\"password\":\"senha1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void refreshExchangesValidTokenForNewOne() throws Exception {
        registerTutor(UserRole.TUTOR);
        String token = loginToken();

        String response = mvc.perform(post("/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("maria@petflow.com"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String renewed = com.jayway.jsonpath.JsonPath.read(response, "$.token");
        mvc.perform(get("/qualquer-rota-protegida").header("Authorization", "Bearer " + renewed))
                .andExpect(status().isNotFound()); // o token renovado autentica
    }

    @Test
    void refreshReadsCurrentRoleFromDatabase() throws Exception {
        registerTutor(UserRole.TUTOR);
        String token = loginToken();
        registerTutor(UserRole.ADMIN); // perfil promovido depois do login

        mvc.perform(post("/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void refreshWithoutTokenReturns401() throws Exception {
        mvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithTamperedTokenReturns401() throws Exception {
        mvc.perform(post("/auth/refresh").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshForRemovedUserReturns401WithCode() throws Exception {
        registerTutor(UserRole.TUTOR);
        String token = loginToken();
        when(tutorRepository.findById(5L)).thenReturn(Optional.empty());

        mvc.perform(post("/auth/refresh").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void loginReturnsTokenAndProfile() throws Exception {
        registerTutor(UserRole.TUTOR);

        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@petflow.com\",\"password\":\"senha1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.role").value("TUTOR"));
    }

    @Test
    void issuedTokenAuthenticatesFollowingRequests() throws Exception {
        registerTutor(UserRole.ADMIN);
        String response = mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@petflow.com\",\"password\":\"senha1234\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.token");

        mvc.perform(get("/qualquer-rota-protegida").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // autenticado: passa pela segurança e não acha a rota
        mvc.perform(get("/qualquer-rota-protegida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        registerTutor(UserRole.TUTOR);

        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@petflow.com\",\"password\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciais inválidas"));
    }

    @Test
    void unknownUserReturns401() throws Exception {
        when(tutorRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());

        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"x@x.com\",\"password\":\"senha1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPayloadReturns400() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"nao-e-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        mvc.perform(get("/qualquer-rota-protegida").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }
}
