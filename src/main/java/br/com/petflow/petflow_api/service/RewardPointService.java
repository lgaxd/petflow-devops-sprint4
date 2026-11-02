package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.entity.Coupon;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Regras de pontos e resgate. Antes ficavam no package PL/SQL PKG_PETFLOW_GAMIFICATION; agora estão em Java para
 * funcionar em qualquer banco. A transação é a do chamador (propagation MANDATORY) e as linhas lidas para decisão
 * são travadas com SELECT ... FOR UPDATE, como no package original.
 */
@Service
@RequiredArgsConstructor
public class RewardPointService {

    private static final Logger log = LoggerFactory.getLogger(RewardPointService.class);

    private final RewardPointRepository rewardPointRepository;
    private final RewardActionRepository rewardActionRepository;
    private final RedeemRepository redeemRepository;
    private final TutorRepository tutorRepository;
    private final PetRepository petRepository;
    private final HealthEventRepository healthEventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public Integer getTutorPoints(Long tutorId) {
        Integer total = rewardPointRepository.sumPointsByTutorId(tutorId);
        return total == null ? 0 : total;
    }

    /** Concede pontos uma única vez por (ação, tipo de referência, id da referência). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void grantPoints(Long tutorId, String actionName, String referenceType, Long referenceId) {
        log.info("Concedendo pontos: ação {}", actionName);
        RewardAction action = rewardActionRepository.findByName(actionName)
                .orElseThrow(() -> new EntityNotFoundException("Ação de recompensa", "nome", actionName));

        Long ownerTutorId;
        int points = action.getPointsValue();

        if ("CADASTRO_PET".equals(actionName) && "PET".equals(referenceType)) {
            Pet pet = petRepository.findByIdForUpdate(referenceId)
                    .orElseThrow(() -> new EntityNotFoundException("Pet", referenceId));
            ownerTutorId = pet.getTutor().getId();
        } else if ("EVENTO_SAUDE_REALIZADO".equals(actionName) && "HEALTH_EVENT".equals(referenceType)) {
            HealthEvent event = healthEventRepository.findByIdForUpdate(referenceId)
                    .orElseThrow(() -> new EntityNotFoundException("Evento de Saúde", referenceId));
            if (event.getStatus() != HealthEventStatus.REALIZADO) {
                throw new BusinessRuleException(
                        "O evento de saúde precisa estar REALIZADO para conceder pontos.", "INVALID_EVENT_STATUS");
            }
            ownerTutorId = event.getPet().getTutor().getId();
            points = event.getEventType().getPointsReward();
        } else if ("ASSINATURA_ATIVA".equals(actionName) && "SUBSCRIPTION".equals(referenceType)) {
            Subscription subscription = subscriptionRepository
                    .findByIdAndStatusForUpdate(referenceId, SubscriptionStatus.ATIVO)
                    .orElseThrow(() -> new EntityNotFoundException("Assinatura", referenceId));
            ownerTutorId = subscription.getPet().getTutor().getId();
        } else {
            throw new BusinessRuleException("Referência de recompensa inválida.", "INVALID_REWARD_REFERENCE");
        }

        if (!ownerTutorId.equals(tutorId)) {
            throw new AccessDeniedException("A referência de recompensa pertence a outro tutor.");
        }

        if (rewardPointRepository.existsByRewardAction_IdAndReferenceTypeAndReferenceId(
                action.getId(), referenceType, referenceId)) {
            return; // idempotente
        }

        rewardPointRepository.save(RewardPoint.builder()
                .tutor(tutorRepository.getReferenceById(tutorId))
                .rewardAction(action)
                .points(points)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build());
    }

    /** Resgata um cupom: valida status, saldo e validade, grava o resgate e debita os pontos. */
    @Transactional(propagation = Propagation.MANDATORY)
    public RedeemResult redeemCoupon(Long tutorId, Long couponId) {
        log.info("Resgatando cupom");
        Tutor tutor = tutorRepository.findByIdForUpdate(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("Cupom", couponId));
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new EntityNotFoundException("Cupom", couponId));

        if (coupon.getStatus() != CouponStatus.DISPONIVEL) {
            throw new BusinessRuleException("Cupom não está disponível para resgate");
        }

        int pointsRequired = coupon.getTemplate().getPointsRequired();
        int available = getTutorPoints(tutorId);
        if (available < pointsRequired) {
            throw new InsufficientPointsException(available, pointsRequired);
        }

        LocalDate expiration = coupon.getExpirationDate();
        if (expiration != null && expiration.isBefore(LocalDate.now())) {
            throw new ExpiredCouponException(coupon.getCode(), expiration);
        }

        RewardAction action = rewardActionRepository.findByName("RESGATE_CUPOM")
                .orElseThrow(() -> new EntityNotFoundException("Ação de recompensa", "nome", "RESGATE_CUPOM"));

        Redeem redeem = redeemRepository.save(Redeem.builder()
                .tutor(tutor)
                .coupon(coupon)
                .pointsUsed(pointsRequired)
                .build());

        coupon.setStatus(CouponStatus.RESGATADO);
        couponRepository.save(coupon);

        rewardPointRepository.save(RewardPoint.builder()
                .tutor(tutor)
                .rewardAction(action)
                .points(-pointsRequired)
                .referenceType("REDEEM")
                .referenceId(redeem.getId())
                .build());

        return new RedeemResult(redeem.getId(), pointsRequired);
    }

    public record RedeemResult(Long redeemId, Integer pointsUsed) {}
}
