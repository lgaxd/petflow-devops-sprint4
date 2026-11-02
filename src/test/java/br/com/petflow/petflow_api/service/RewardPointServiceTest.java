package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.entity.CouponTemplate;
import br.com.petflow.petflow_api.entity.EventType;
import br.com.petflow.petflow_api.entity.HealthEvent;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Redeem;
import br.com.petflow.petflow_api.entity.RewardAction;
import br.com.petflow.petflow_api.entity.RewardPoint;
import br.com.petflow.petflow_api.entity.Subscription;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.CouponStatus;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.enums.SubscriptionStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.exception.ExpiredCouponException;
import br.com.petflow.petflow_api.exception.InsufficientPointsException;
import br.com.petflow.petflow_api.repository.CouponRepository;
import br.com.petflow.petflow_api.repository.HealthEventRepository;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.RedeemRepository;
import br.com.petflow.petflow_api.repository.RewardActionRepository;
import br.com.petflow.petflow_api.repository.RewardPointRepository;
import br.com.petflow.petflow_api.repository.SubscriptionRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de pontos e resgate que antes eram do package PL/SQL, agora testadas sem banco. */
@ExtendWith(MockitoExtension.class)
class RewardPointServiceTest {

    @Mock private RewardPointRepository rewardPointRepository;
    @Mock private RewardActionRepository rewardActionRepository;
    @Mock private RedeemRepository redeemRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private PetRepository petRepository;
    @Mock private HealthEventRepository healthEventRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CouponRepository couponRepository;

    @InjectMocks private RewardPointService service;

    private final Tutor tutor = Tutor.builder().id(1L).build();
    private final Pet pet = Pet.builder().id(10L).tutor(tutor).build();

    private RewardAction action(Long id, String name, int points) {
        return RewardAction.builder().id(id).name(name).pointsValue(points).build();
    }

    // ---------- getTutorPoints ----------

    @Test
    void balanceIsZeroWhenTutorHasNoPoints() {
        when(rewardPointRepository.sumPointsByTutorId(1L)).thenReturn(null);
        assertEquals(0, service.getTutorPoints(1L));
    }

    // ---------- grantPoints ----------

    @Test
    void petRegistrationGrantsActionPoints() {
        when(rewardActionRepository.findByName("CADASTRO_PET")).thenReturn(Optional.of(action(1L, "CADASTRO_PET", 5)));
        when(petRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pet));
        when(rewardPointRepository.existsByRewardAction_IdAndReferenceTypeAndReferenceId(1L, "PET", 10L)).thenReturn(false);
        when(tutorRepository.getReferenceById(1L)).thenReturn(tutor);

        service.grantPoints(1L, "CADASTRO_PET", "PET", 10L);

        ArgumentCaptor<RewardPoint> saved = ArgumentCaptor.forClass(RewardPoint.class);
        verify(rewardPointRepository).save(saved.capture());
        assertEquals(5, saved.getValue().getPoints());
        assertEquals("PET", saved.getValue().getReferenceType());
        assertEquals(10L, saved.getValue().getReferenceId());
    }

    @Test
    void grantIsIdempotent() {
        when(rewardActionRepository.findByName("CADASTRO_PET")).thenReturn(Optional.of(action(1L, "CADASTRO_PET", 5)));
        when(petRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pet));
        when(rewardPointRepository.existsByRewardAction_IdAndReferenceTypeAndReferenceId(1L, "PET", 10L)).thenReturn(true);

        service.grantPoints(1L, "CADASTRO_PET", "PET", 10L);

        verify(rewardPointRepository, never()).save(any());
    }

    @Test
    void grantForAnotherTutorIsForbidden() {
        when(rewardActionRepository.findByName("CADASTRO_PET")).thenReturn(Optional.of(action(1L, "CADASTRO_PET", 5)));
        when(petRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(pet));

        assertThrows(AccessDeniedException.class, () -> service.grantPoints(2L, "CADASTRO_PET", "PET", 10L));
        verify(rewardPointRepository, never()).save(any());
    }

    @Test
    void grantForUnknownReferenceIsNotFound() {
        when(rewardActionRepository.findByName("CADASTRO_PET")).thenReturn(Optional.of(action(1L, "CADASTRO_PET", 5)));
        when(petRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.grantPoints(1L, "CADASTRO_PET", "PET", 99L));
    }

    @Test
    void grantWithMismatchedActionAndReferenceIsRejected() {
        when(rewardActionRepository.findByName("CADASTRO_PET")).thenReturn(Optional.of(action(1L, "CADASTRO_PET", 5)));

        BusinessRuleException error =
                assertThrows(BusinessRuleException.class, () -> service.grantPoints(1L, "CADASTRO_PET", "HEALTH_EVENT", 1L));
        assertEquals("INVALID_REWARD_REFERENCE", error.getCode());
    }

    @Test
    void healthEventPointsComeFromEventTypeAndMustBeRealized() {
        RewardAction action = action(2L, "EVENTO_SAUDE_REALIZADO", 10);
        EventType type = EventType.builder().id(1L).pointsReward(7).build();
        HealthEvent scheduled = HealthEvent.builder().id(20L).pet(pet).eventType(type)
                .status(HealthEventStatus.AGENDADO).build();
        HealthEvent done = HealthEvent.builder().id(21L).pet(pet).eventType(type)
                .status(HealthEventStatus.REALIZADO).build();
        when(rewardActionRepository.findByName("EVENTO_SAUDE_REALIZADO")).thenReturn(Optional.of(action));
        when(healthEventRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(scheduled));
        when(healthEventRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(done));
        when(tutorRepository.getReferenceById(1L)).thenReturn(tutor);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.grantPoints(1L, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", 20L));
        assertEquals("INVALID_EVENT_STATUS", error.getCode());

        service.grantPoints(1L, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", 21L);
        ArgumentCaptor<RewardPoint> saved = ArgumentCaptor.forClass(RewardPoint.class);
        verify(rewardPointRepository).save(saved.capture());
        assertEquals(7, saved.getValue().getPoints());
    }

    @Test
    void onlyActiveSubscriptionsGrantPoints() {
        when(rewardActionRepository.findByName("ASSINATURA_ATIVA")).thenReturn(Optional.of(action(3L, "ASSINATURA_ATIVA", 15)));
        when(subscriptionRepository.findByIdAndStatusForUpdate(30L, SubscriptionStatus.ATIVO)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.grantPoints(1L, "ASSINATURA_ATIVA", "SUBSCRIPTION", 30L));

        Subscription active = Subscription.builder().id(31L).pet(pet).status(SubscriptionStatus.ATIVO).build();
        when(subscriptionRepository.findByIdAndStatusForUpdate(31L, SubscriptionStatus.ATIVO)).thenReturn(Optional.of(active));
        when(tutorRepository.getReferenceById(1L)).thenReturn(tutor);

        service.grantPoints(1L, "ASSINATURA_ATIVA", "SUBSCRIPTION", 31L);

        ArgumentCaptor<RewardPoint> saved = ArgumentCaptor.forClass(RewardPoint.class);
        verify(rewardPointRepository).save(saved.capture());
        assertEquals(15, saved.getValue().getPoints());
    }

    // ---------- redeemCoupon ----------

    private Coupon coupon(CouponStatus status, LocalDate expiration, int pointsRequired) {
        CouponTemplate template = CouponTemplate.builder().id(1L).pointsRequired(pointsRequired).build();
        return Coupon.builder().id(5L).code("PET10").status(status).expirationDate(expiration).template(template).build();
    }

    private void stubRedeemLocks(Coupon coupon) {
        when(tutorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tutor));
        when(couponRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(coupon));
    }

    @Test
    void redeemDebitsPointsAndMarksCouponRedeemed() {
        Coupon coupon = coupon(CouponStatus.DISPONIVEL, LocalDate.now().plusDays(10), 30);
        stubRedeemLocks(coupon);
        when(rewardPointRepository.sumPointsByTutorId(1L)).thenReturn(100);
        when(rewardActionRepository.findByName("RESGATE_CUPOM")).thenReturn(Optional.of(action(4L, "RESGATE_CUPOM", 0)));
        when(redeemRepository.save(any(Redeem.class))).thenAnswer(call -> {
            Redeem redeem = call.getArgument(0);
            redeem.setId(77L);
            return redeem;
        });

        RewardPointService.RedeemResult result = service.redeemCoupon(1L, 5L);

        assertEquals(77L, result.redeemId());
        assertEquals(30, result.pointsUsed());
        assertEquals(CouponStatus.RESGATADO, coupon.getStatus());
        ArgumentCaptor<RewardPoint> debit = ArgumentCaptor.forClass(RewardPoint.class);
        verify(rewardPointRepository).save(debit.capture());
        assertEquals(-30, debit.getValue().getPoints());
        assertEquals("REDEEM", debit.getValue().getReferenceType());
        assertEquals(77L, debit.getValue().getReferenceId());
    }

    @Test
    void redeemWithoutEnoughPointsFails() {
        stubRedeemLocks(coupon(CouponStatus.DISPONIVEL, LocalDate.now().plusDays(10), 100));
        when(rewardPointRepository.sumPointsByTutorId(1L)).thenReturn(10);

        InsufficientPointsException error =
                assertThrows(InsufficientPointsException.class, () -> service.redeemCoupon(1L, 5L));
        assertEquals(10, error.getAvailablePoints());
        assertEquals(100, error.getRequiredPoints());
        verify(redeemRepository, never()).save(any());
    }

    @Test
    void redeemOfExpiredCouponFails() {
        stubRedeemLocks(coupon(CouponStatus.DISPONIVEL, LocalDate.of(2020, 1, 1), 30));
        when(rewardPointRepository.sumPointsByTutorId(1L)).thenReturn(100);

        ExpiredCouponException error = assertThrows(ExpiredCouponException.class, () -> service.redeemCoupon(1L, 5L));
        assertEquals("PET10", error.getCouponCode());
        assertEquals(LocalDate.of(2020, 1, 1), error.getExpirationDate());
    }

    @Test
    void redeemOfUnavailableCouponFails() {
        stubRedeemLocks(coupon(CouponStatus.RESGATADO, null, 30));

        assertThrows(BusinessRuleException.class, () -> service.redeemCoupon(1L, 5L));
    }

    @Test
    void redeemOfUnknownCouponIsNotFound() {
        when(tutorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tutor));
        when(couponRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.redeemCoupon(1L, 5L));
    }
}
