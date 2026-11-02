package br.com.petflow.petflow_api.exception;

import lombok.Getter;

@Getter
public class InsufficientPointsException extends BusinessRuleException {

    private final Integer availablePoints;
    private final Integer requiredPoints;

    public InsufficientPointsException(Integer availablePoints, Integer requiredPoints) {
        super(String.format("Pontos insuficientes para resgate. Disponível: %d | Necessário: %d", 
                availablePoints, requiredPoints), "INSUFFICIENT_POINTS");
        this.availablePoints = availablePoints;
        this.requiredPoints = requiredPoints;
    }
}