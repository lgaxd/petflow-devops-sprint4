package br.com.petflow.petflow_api.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Long entityId;

    public EntityNotFoundException(String entityName, Long entityId) {
        super(String.format("%s não encontrado(a) com ID: %d", entityName, entityId));
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(String.format("%s não encontrado(a) com %s: %s", entityName, field, value));
        this.entityName = entityName;
        this.entityId = null;
    }
}