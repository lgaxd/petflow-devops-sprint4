package br.com.petflow.petflow_api.enums;

import java.util.Locale;
import java.util.StringJoiner;

/**
 * Converte texto recebido da API em constantes de enum, com mensagem padronizada
 * listando os valores aceitos quando o texto é inválido.
 */
public final class EnumParser {

    private EnumParser() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value != null) {
            try {
                return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // cai na mensagem padronizada abaixo
            }
        }
        StringJoiner allowed = new StringJoiner(", ");
        for (E constant : type.getEnumConstants()) {
            allowed.add(constant.name());
        }
        throw new IllegalArgumentException("Status inválido. Valores permitidos: " + allowed);
    }

    public static <E extends Enum<E>> E parseOrNull(Class<E> type, String value) {
        return value == null || value.isBlank() ? null : parse(type, value);
    }

    public static <E extends Enum<E>> E parseOrDefault(Class<E> type, String value, E defaultValue) {
        E parsed = parseOrNull(type, value);
        return parsed != null ? parsed : defaultValue;
    }
}
