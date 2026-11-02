package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.dto.CouponCatalogDTO;
import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.enums.CouponStatus;
import br.com.petflow.petflow_api.repository.CouponRepository;
import br.com.petflow.petflow_api.repository.CouponTemplateRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Tag("integration")
class CouponCatalogRepositoryTest {

    @Autowired
    private CouponTemplateRepository couponTemplateRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void availableCatalogIdsReferToRedeemableCoupons() {
        Page<CouponCatalogDTO> catalog = couponTemplateRepository.findAvailableCoupons(Pageable.unpaged());

        assertNotNull(catalog);
        assertTrue(catalog.hasContent(), "O seed deve possuir ao menos um cupom disponível");

        for (CouponCatalogDTO item : catalog.getContent()) {
            Coupon coupon = couponRepository.findById(item.id()).orElse(null);

            assertNotNull(coupon, "O ID do catálogo deve ser de um Coupon individual");
            assertEquals(coupon.getId(), item.id());
            assertEquals(coupon.getCode(), item.code());
            assertEquals(CouponStatus.DISPONIVEL, coupon.getStatus());
        }
    }
}
