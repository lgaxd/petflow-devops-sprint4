package br.com.petflow.petflow_api.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CouponCatalogDTO(
    Long id,
    String code,
    String title,
    Integer pointsRequired,
    String discountType,
    Double discountValue,
    LocalDate expirationDate,
    Boolean available
) {}