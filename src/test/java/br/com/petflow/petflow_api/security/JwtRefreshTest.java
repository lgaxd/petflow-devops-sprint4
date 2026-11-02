package br.com.petflow.petflow_api.security;

import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renovação de token: preservação do início da sessão e limite de duração. */
class JwtRefreshTest {

    private static final String SECRET = "unit-test-secret-with-at-least-thirty-two-chars";
    private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;

    private JwtService service(long maxSessionMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "expirationMs", 60_000L);
        ReflectionTestUtils.setField(service, "maxSessionMs", maxSessionMs);
        return service;
    }

    private Tutor tutor(UserRole role) {
        return Tutor.builder().id(5L).name("Maria").email("maria@petflow.com").role(role).build();
    }

    @Test
    void newTokenStartsASessionNow() {
        JwtService service = service(7 * ONE_DAY_MS);

        DecodedJWT decoded = service.validateToken(service.generateToken(tutor(UserRole.TUTOR)));

        Instant start = decoded.getClaim("sessionStart").asInstant();
        assertNotNull(start);
        assertTrue(Math.abs(ChronoUnit.SECONDS.between(start, Instant.now())) <= 2);
        assertFalse(service.isSessionExpired(decoded));
    }

    @Test
    void refreshedTokenKeepsOriginalSessionStartAndUsesCurrentData() {
        JwtService service = service(7 * ONE_DAY_MS);
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String original = JWT.create()
                .withSubject("maria@petflow.com").withClaim("id", 5L).withClaim("name", "Maria")
                .withClaim("role", "TUTOR").withClaim("sessionStart", twoDaysAgo)
                .withIssuedAt(new Date()).withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(SECRET));
        DecodedJWT current = service.validateToken(original);

        DecodedJWT refreshed = service.validateToken(
                service.generateRefreshedToken(tutor(UserRole.ADMIN), current));

        assertEquals(twoDaysAgo, refreshed.getClaim("sessionStart").asInstant());
        assertEquals("ADMIN", refreshed.getClaim("role").asString()); // perfil atual do banco
    }

    @Test
    void sessionOlderThanTheLimitCannotBeRenewed() {
        JwtService service = service(ONE_DAY_MS);
        String old = JWT.create()
                .withSubject("maria@petflow.com").withClaim("id", 5L).withClaim("name", "Maria")
                .withClaim("role", "TUTOR").withClaim("sessionStart", Instant.now().minus(2, ChronoUnit.DAYS))
                .withIssuedAt(new Date()).withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(SECRET));

        assertTrue(service.isSessionExpired(service.validateToken(old)));
    }

    @Test
    void tokenWithoutSessionStartFallsBackToIssueDate() {
        JwtService service = service(7 * ONE_DAY_MS);
        String legacyRecent = JWT.create().withSubject("a@b.com").withClaim("role", "TUTOR")
                .withIssuedAt(new Date()).withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(SECRET));
        String legacyOld = JWT.create().withSubject("a@b.com").withClaim("role", "TUTOR")
                .withIssuedAt(Date.from(Instant.now().minus(8, ChronoUnit.DAYS)))
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .sign(Algorithm.HMAC256(SECRET));

        assertFalse(service.isSessionExpired(service.validateToken(legacyRecent)));
        assertTrue(service.isSessionExpired(service.validateToken(legacyOld)));
    }

    @Test
    void expirationIsExposedForValidTokensOnly() {
        JwtService service = service(7 * ONE_DAY_MS);
        String token = service.generateToken(tutor(UserRole.TUTOR));

        Instant expiresAt = service.extractExpiration(token);

        assertNotNull(expiresAt);
        assertTrue(expiresAt.isAfter(Instant.now()));
        assertNull(service.extractExpiration("not.a.jwt"));
    }
}
