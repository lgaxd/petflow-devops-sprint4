package br.com.petflow.petflow_api.exception;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class ExpiredCouponException extends BusinessRuleException {

    private final String couponCode;
    private final LocalDate expirationDate;

    public ExpiredCouponException(String couponCode, LocalDate expirationDate) {
        super(String.format("Cupom %s expirado em %s", couponCode, expirationDate), "EXPIRED_COUPON");
        this.couponCode = couponCode;
        this.expirationDate = expirationDate;
    }
}