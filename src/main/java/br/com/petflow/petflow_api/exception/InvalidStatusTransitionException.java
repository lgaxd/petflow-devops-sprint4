package br.com.petflow.petflow_api.exception;

import lombok.Getter;

@Getter
public class InvalidStatusTransitionException extends BusinessRuleException {

    private final String currentStatus;
    private final String targetStatus;
    private final String entityType;

    public InvalidStatusTransitionException(String entityType, String currentStatus, String targetStatus) {
        super(String.format("Transição de status inválida para %s: %s -> %s", 
                entityType, currentStatus, targetStatus), "INVALID_STATUS_TRANSITION");
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.entityType = entityType;
    }
}