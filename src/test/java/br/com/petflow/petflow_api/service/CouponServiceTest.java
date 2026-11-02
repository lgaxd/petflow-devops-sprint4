package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.CouponRequestDTO;
import br.com.petflow.petflow_api.dto.CouponResponseDTO;
import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.entity.CouponTemplate;
import br.com.petflow.petflow_api.enums.CouponStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.CouponRepository;
import br.com.petflow.petflow_api.repository.CouponTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @InjectMocks
    private CouponService couponService;

    private final CouponTemplate template = CouponTemplate.builder().id(1L).build();

    private Coupon coupon(CouponStatus status) {
        return Coupon.builder().id(9L).code("PET10").status(status).template(template).build();
    }

    private CouponRequestDTO request(String status) {
        return CouponRequestDTO.builder().code("PET10").status(status)
                .expirationDate(LocalDate.now().plusDays(30)).templateId(1L).build();
    }

    @Test
    void createDefaultsToDisponivelWhenStatusIsBlank() {
        when(couponRepository.findByCode("PET10")).thenReturn(Optional.empty());
        when(couponTemplateRepository.findById(1L)).thenReturn(Optional.of(template));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));

        CouponResponseDTO response = couponService.create(request(" "));

        assertEquals(CouponStatus.DISPONIVEL, response.status());
        assertEquals(1L, response.templateId());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(couponRepository.findByCode("PET10")).thenReturn(Optional.of(coupon(CouponStatus.DISPONIVEL)));

        assertThrows(DuplicateResourceException.class, () -> couponService.create(request("DISPONIVEL")));
        verify(couponRepository, never()).save(any());
    }

    @Test
    void createFailsWhenTemplateDoesNotExist() {
        when(couponRepository.findByCode("PET10")).thenReturn(Optional.empty());
        when(couponTemplateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> couponService.create(request("DISPONIVEL")));
    }

    @Test
    void createRejectsUnknownStatusListingAllowedValues() {
        when(couponRepository.findByCode("PET10")).thenReturn(Optional.empty());
        when(couponTemplateRepository.findById(1L)).thenReturn(Optional.of(template));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> couponService.create(request("INVENTADO")));
        assertEquals("Status inválido. Valores permitidos: DISPONIVEL, RESGATADO, UTILIZADO", error.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"DISPONIVEL,UTILIZADO", "DISPONIVEL,RESGATADO", "DISPONIVEL,DISPONIVEL", "utilizado,UTILIZADO"})
    void allowedTransitionsAreApplied(String current, String target) {
        when(couponRepository.findById(9L)).thenReturn(Optional.of(coupon(CouponStatus.valueOf(current.toUpperCase()))));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(CouponStatus.valueOf(target), couponService.updateStatus(9L, target).status());
    }

    @ParameterizedTest
    @CsvSource({"UTILIZADO,DISPONIVEL", "RESGATADO,UTILIZADO", "RESGATADO,DISPONIVEL"})
    void closedCouponsCannotChangeStatus(String current, String target) {
        when(couponRepository.findById(9L)).thenReturn(Optional.of(coupon(CouponStatus.valueOf(current))));

        BusinessRuleException error =
                assertThrows(BusinessRuleException.class, () -> couponService.updateStatus(9L, target));
        assertEquals("INVALID_COUPON_STATUS_TRANSITION", error.getCode());
        verify(couponRepository, never()).save(any());
    }

    @Test
    void availableCouponCannotGoBackThroughInvalidTarget() {
        // DISPONIVEL -> DISPONIVEL é idempotente, qualquer outro destino fora do conjunto permitido não existe no enum
        when(couponRepository.findById(9L)).thenReturn(Optional.of(coupon(CouponStatus.DISPONIVEL)));

        assertThrows(IllegalArgumentException.class, () -> couponService.updateStatus(9L, "CANCELADO"));
    }

    @Test
    void listWithStatusFiltersByEnum() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.<CouponResponseDTO>of(), pageable, 0);
        when(couponRepository.findByStatus(CouponStatus.UTILIZADO, pageable)).thenReturn(page);

        assertEquals(page, couponService.findAll("utilizado", pageable));
    }

    @Test
    void listWithoutStatusReturnsEverything() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.<CouponResponseDTO>of(), pageable, 0);
        when(couponRepository.findAllProjected(pageable)).thenReturn(page);

        assertEquals(page, couponService.findAll(null, pageable));
    }

    @Test
    void deleteFailsForUnknownCoupon() {
        when(couponRepository.existsById(404L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> couponService.delete(404L));
        verify(couponRepository, never()).deleteById(any());
    }
}
