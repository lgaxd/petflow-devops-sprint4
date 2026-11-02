package br.com.petflow.petflow_api.security;

import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

/**
 * Responsável por gerar e validar os tokens JWT usados para autenticar
 * as requisições à API. O token carrega o email (subject) e o papel (role)
 * do usuário, para que o filtro de autenticação consiga reconstruir as
 * autoridades sem precisar consultar o banco a cada requisição.
 *
 * <p>Cada token também registra quando a sessão começou (claim {@code sessionStart}).
 * A renovação preserva esse instante, e a sessão deixa de poder ser renovada depois
 * de {@code jwt.max-session-ms}: renovar sem parar não gera uma sessão eterna.
 */
@Service
public class JwtService {

    static final int MIN_SECRET_LENGTH = 32;
    static final String SESSION_START_CLAIM = "sessionStart";
    private static final long DEFAULT_MAX_SESSION_MS = 7L * 24 * 60 * 60 * 1000;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${jwt.max-session-ms:604800000}")
    private long maxSessionMs = DEFAULT_MAX_SESSION_MS;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) deve ter no mínimo " + MIN_SECRET_LENGTH + " caracteres.");
        }
    }

    public String generateToken(Tutor tutor) {
        return buildToken(tutor.getId(), tutor.getEmail(), tutor.getName(), tutor.getRole(), Instant.now());
    }

    public String generateToken(AuthenticatedTutor tutor) {
        return buildToken(tutor.getId(), tutor.getEmail(), tutor.getName(), tutor.getRole(), Instant.now());
    }

    /** Novo token para o usuário (dados atuais do banco), mantendo o início da sessão do token anterior. */
    public String generateRefreshedToken(Tutor tutor, DecodedJWT current) {
        return buildToken(tutor.getId(), tutor.getEmail(), tutor.getName(), tutor.getRole(), sessionStartOf(current));
    }

    /** {@code true} quando a sessão já passou do tempo máximo e não pode mais ser renovada. */
    public boolean isSessionExpired(DecodedJWT decoded) {
        return !Instant.now().isBefore(sessionStartOf(decoded).plusMillis(maxSessionMs));
    }

    public DecodedJWT validateToken(String token) {
        try {
            return JWT.require(algorithm()).build().verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        DecodedJWT decoded = validateToken(token);
        return decoded != null ? decoded.getSubject() : null;
    }

    public Instant extractExpiration(String token) {
        DecodedJWT decoded = validateToken(token);
        return decoded != null ? decoded.getExpiresAtAsInstant() : null;
    }

    private String buildToken(Long id, String email, String name, UserRole role, Instant sessionStart) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(email)
                .withClaim("id", id)
                .withClaim("name", name)
                .withClaim("role", role.name())
                .withClaim(SESSION_START_CLAIM, sessionStart)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusMillis(expirationMs)))
                .sign(algorithm());
    }

    /** Tokens emitidos antes desta claim existir usam a data de emissão como início da sessão. */
    private Instant sessionStartOf(DecodedJWT decoded) {
        Instant start = decoded.getClaim(SESSION_START_CLAIM).asInstant();
        if (start != null) {
            return start;
        }
        Instant issuedAt = decoded.getIssuedAtAsInstant();
        return issuedAt != null ? issuedAt : Instant.EPOCH;
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(secret);
    }
}
