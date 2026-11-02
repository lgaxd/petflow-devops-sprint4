package br.com.petflow.petflow_api.exception;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String message) {
        super(message);
        this.code = "BUSINESS_RULE_VIOLATION";
    }

    public BusinessRuleException(String message, String code) {
        super(message);
        this.code = code;
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_RULE_VIOLATION";
    }
}