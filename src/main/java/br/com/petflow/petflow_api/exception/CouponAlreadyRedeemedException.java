package br.com.petflow.petflow_api.exception;

import lombok.Getter;

@Getter
public class CouponAlreadyRedeemedException extends BusinessRuleException {

    private final String couponCode;

    public CouponAlreadyRedeemedException(String couponCode) {
        super(String.format("Cupom %s já foi resgatado anteriormente", couponCode), "COUPON_ALREADY_REDEEMED");
        this.couponCode = couponCode;
    }
}