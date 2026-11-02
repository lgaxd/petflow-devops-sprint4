package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.config.SecurityConfig;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.GlobalExceptionHandler;
import br.com.petflow.petflow_api.security.AuthenticatedTutor;
import br.com.petflow.petflow_api.security.JwtAccessDeniedHandler;
import br.com.petflow.petflow_api.security.JwtAuthenticationEntryPoint;
import br.com.petflow.petflow_api.security.JwtAuthenticationFilter;
import br.com.petflow.petflow_api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Base dos testes de controller: sobe apenas a camada web com a configuração
 * real de segurança (filtro JWT, regras por perfil e tratamento global de erros).
 */
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtService.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-with-at-least-thirty-two-characters",
        "jwt.expiration-ms=3600000",
        "CORS_ALLOWED_ORIGINS=http://localhost:5173"
})
abstract class ControllerTestSupport {

    protected static final long TUTOR_ID = 7L;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    protected String tutorToken() {
        return bearer(new AuthenticatedTutor(TUTOR_ID, "tutor@petflow.com", "Tutor", null, UserRole.TUTOR));
    }

    protected String adminToken() {
        return bearer(new AuthenticatedTutor(1L, "admin@petflow.com", "Admin", null, UserRole.ADMIN));
    }

    private String bearer(AuthenticatedTutor user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String token, String body) {
        return request.header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    protected <T> PageImpl<T> page(T item) {
        return new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
    }
}
