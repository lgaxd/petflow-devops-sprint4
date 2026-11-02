package br.com.petflow.petflow_api.security;

import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.EnumParser;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.enums.UserRole;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-thirty-two-chars";

    private JwtService service(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expirationMs", expirationMs);
        return service;
    }

    @Test
    void tokenCarriesIdentityAndRole() {
        JwtService service = service(SECRET, 60_000);
        Tutor tutor = Tutor.builder().id(5L).name("Maria").email("maria@petflow.com").role(UserRole.ADMIN).build();

        DecodedJWT decoded = service.validateToken(service.generateToken(tutor));

        assertNotNull(decoded);
        assertEquals("maria@petflow.com", decoded.getSubject());
        assertEquals(5L, decoded.getClaim("id").asLong());
        assertEquals("ADMIN", decoded.getClaim("role").asString());
    }

    @Test
    void authenticatedTutorOverloadProducesTheSameClaims() {
        JwtService service = service(SECRET, 60_000);
        AuthenticatedTutor user = new AuthenticatedTutor(8L, "t@petflow.com", "T", null, UserRole.TUTOR);

        assertEquals("t@petflow.com", service.extractEmail(service.generateToken(user)));
    }

    @Test
    void expiredAndTamperedTokensAreRejected() {
        JwtService expired = service(SECRET, -1_000);
        AuthenticatedTutor user = new AuthenticatedTutor(8L, "t@petflow.com", "T", null, UserRole.TUTOR);
        String expiredToken = expired.generateToken(user);

        assertNull(expired.validateToken(expiredToken));
        assertNull(service(SECRET, 60_000).validateToken("not.a.jwt"));
        assertNull(service("another-secret-with-at-least-thirty-two-chars", 60_000)
                .validateToken(service(SECRET, 60_000).generateToken(user)));
        assertNull(service(SECRET, 60_000).extractEmail("garbage"));
    }

    @Test
    void shortSecretsAreRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> service("curto", 1).validateSecret());
        assertThrows(IllegalStateException.class, () -> service(null, 1).validateSecret());
        service(SECRET, 1).validateSecret();
    }

    @Test
    void enumParserIsCaseInsensitiveAndListsAllowedValues() {
        assertEquals(HealthEventStatus.REALIZADO, EnumParser.parse(HealthEventStatus.class, " realizado "));
        assertNull(EnumParser.parseOrNull(HealthEventStatus.class, "  "));
        assertEquals(HealthEventStatus.AGENDADO, EnumParser.parseOrDefault(HealthEventStatus.class, null, HealthEventStatus.AGENDADO));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> EnumParser.parse(HealthEventStatus.class, "x"));
        assertEquals("Status inválido. Valores permitidos: AGENDADO, REALIZADO, CANCELADO", error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> EnumParser.parse(HealthEventStatus.class, null));
    }
}
