package br.com.petflow.petflow_api.exception;

import lombok.Getter;

/** Token JWT inválido ou sessão que não pode mais ser renovada (HTTP 401). */
@Getter
public class InvalidTokenException extends RuntimeException {

    public static final String TOKEN_INVALID = "TOKEN_INVALID";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    private final String code;

    public InvalidTokenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static InvalidTokenException invalid() {
        return new InvalidTokenException(TOKEN_INVALID, "Token inválido ou expirado. Faça login novamente.");
    }

    public static InvalidTokenException sessionExpired() {
        return new InvalidTokenException(SESSION_EXPIRED,
                "A sessão atingiu o tempo máximo e não pode ser renovada. Faça login novamente.");
    }
}
