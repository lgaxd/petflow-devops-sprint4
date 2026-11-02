package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.LoginRequestDTO;
import br.com.petflow.petflow_api.dto.LoginResponseDTO;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.InvalidTokenException;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.security.AuthenticatedTutor;
import br.com.petflow.petflow_api.security.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-thirty-two-chars";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TutorRepository tutorRepository;

    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);
        authService = new AuthService(authenticationManager, tutorRepository, jwtService);
    }

    private Tutor tutor(UserRole role) {
        return Tutor.builder().id(5L).name("Maria").email("maria@petflow.com").role(role).build();
    }

    private String tokenFor(Tutor tutor) {
        return "Bearer " + jwtService.generateToken(tutor);
    }

    @Test
    void loginReturnsTokenProfileAndExpiration() {
        AuthenticatedTutor principal = new AuthenticatedTutor(5L, "maria@petflow.com", "Maria", "hash", UserRole.TUTOR);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        LoginResponseDTO response = authService.login(new LoginRequestDTO("maria@petflow.com", "senha1234"));

        assertEquals(5L, response.id());
        assertEquals(UserRole.TUTOR, response.role());
        assertNotNull(response.token());
        assertTrue(response.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void refreshKeepsSessionStartAndRereadsTheUser() {
        Tutor before = tutor(UserRole.TUTOR);
        String header = tokenFor(before);
        DecodedJWT original = jwtService.validateToken(header.substring(7));
        when(tutorRepository.findById(5L)).thenReturn(Optional.of(tutor(UserRole.ADMIN)));

        LoginResponseDTO response = authService.refresh(header);

        assertEquals(UserRole.ADMIN, response.role());
        DecodedJWT renewed = jwtService.validateToken(response.token());
        assertEquals(original.getClaim("sessionStart").asInstant(), renewed.getClaim("sessionStart").asInstant());
        assertNotNull(response.expiresAt());
    }

    @Test
    void refreshRejectsMissingMalformedAndTamperedTokens() {
        assertEquals(InvalidTokenException.TOKEN_INVALID,
                assertThrows(InvalidTokenException.class, () -> authService.refresh(null)).getCode());
        assertEquals(InvalidTokenException.TOKEN_INVALID,
                assertThrows(InvalidTokenException.class, () -> authService.refresh("Basic abc")).getCode());
        assertEquals(InvalidTokenException.TOKEN_INVALID,
                assertThrows(InvalidTokenException.class, () -> authService.refresh("Bearer abc.def.ghi")).getCode());
    }

    @Test
    void refreshRejectsRemovedUser() {
        String header = tokenFor(tutor(UserRole.TUTOR));
        when(tutorRepository.findById(5L)).thenReturn(Optional.empty());

        InvalidTokenException error = assertThrows(InvalidTokenException.class, () -> authService.refresh(header));

        assertEquals(InvalidTokenException.TOKEN_INVALID, error.getCode());
    }

    @Test
    void refreshRejectsSessionBeyondTheLimit() {
        ReflectionTestUtils.setField(jwtService, "maxSessionMs", -1L); // limite já vencido
        String header = tokenFor(tutor(UserRole.TUTOR));

        InvalidTokenException error = assertThrows(InvalidTokenException.class, () -> authService.refresh(header));

        assertEquals(InvalidTokenException.SESSION_EXPIRED, error.getCode());
    }
}
