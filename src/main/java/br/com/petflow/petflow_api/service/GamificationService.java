package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.*;
import br.com.petflow.petflow_api.entity.*;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.*;
import br.com.petflow.petflow_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final TutorRepository tutorRepository;
    private final RewardPointRepository rewardPointRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final RedeemRepository redeemRepository;
    private final PetRepository petRepository;
    private final RewardPointService rewardPointService;

    @Transactional(readOnly = true)
    public TutorPointsDTO getTutorPoints(Long tutorId) {
        SecurityUtils.checkOwnership(tutorId);

        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new EntityNotFoundException("Tutor", tutorId));

        Integer totalPoints = rewardPointService.getTutorPoints(tutorId);

        Page<PointHistoryDTO> history = rewardPointRepository.findHistoryByTutorId(tutorId, Pageable.ofSize(50));

        return TutorPointsDTO.builder()
                .tutorId(tutor.getId())
                .tutorName(tutor.getName())
                .totalPoints(totalPoints)
                .history(history.getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public PetRiskDTO getPetRisk(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new EntityNotFoundException("Pet", petId));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(pet.getTutor().getId());
        }

        Integer score = calculateRiskScore(pet);
        String level = getRiskLevel(score);
        String description = getRiskDescription(level);

        return PetRiskDTO.builder()
                .petId(pet.getId())
                .petName(pet.getName())
                .score(score)
                .riskLevel(level)
                .riskDescription(description)
                .build();
    }

    private Integer calculateRiskScore(Pet pet) {
        int score = 0;

        if (pet.getBirthDate() != null) {
            int age = java.time.Period.between(pet.getBirthDate(), LocalDate.now()).getYears();
            if (age > 10) score += 20;
            else if (age > 7) score += 10;
            else if (age > 5) score += 5;
        }

        // Peso - usando BigDecimal corretamente
        if (pet.getWeight() != null) {
            BigDecimal weight = pet.getWeight();
            if (weight.compareTo(BigDecimal.valueOf(3.0)) < 0 || weight.compareTo(BigDecimal.valueOf(30.0)) > 0) {
                score += 15;
            } else if (weight.compareTo(BigDecimal.valueOf(5.0)) < 0 || weight.compareTo(BigDecimal.valueOf(25.0)) > 0) {
                score += 8;
            }
        }

        return Math.min(score, 100);
    }

    private String getRiskLevel(Integer score) {
        if (score >= 61) return "ALTO";
        if (score >= 31) return "MEDIO";
        return "BAIXO";
    }

    private String getRiskDescription(String level) {
        return switch (level) {
            case "ALTO" -> "Requer acompanhamento veterinário próximo";
            case "MEDIO" -> "Atenção recomendada para cuidados preventivos";
            default -> "Pet saudável com baixo risco";
        };
    }

    @Transactional(readOnly = true)
    public Page<CouponCatalogDTO> getAvailableCoupons(Pageable pageable) {
        return couponTemplateRepository.findAvailableCoupons(pageable);
    }

    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public RedeemResponseDTO redeemCoupon(Long tutorId, Long couponId) {
        SecurityUtils.checkOwnership(tutorId);

        RewardPointService.RedeemResult result = rewardPointService.redeemCoupon(tutorId, couponId);

        Redeem redeem = redeemRepository.findById(result.redeemId())
                .orElseThrow(() -> new EntityNotFoundException("Resgate", result.redeemId()));
        return toResponseDTO(redeem);
    }

    private RedeemResponseDTO toResponseDTO(Redeem redeem) {
        return RedeemResponseDTO.builder()
                .id(redeem.getId())
                .pointsUsed(redeem.getPointsUsed())
                .createdAt(redeem.getCreatedAt())
                .tutorId(redeem.getTutor().getId())
                .tutorName(redeem.getTutor().getName())
                .couponId(redeem.getCoupon().getId())
                .couponCode(redeem.getCoupon().getCode())
                .build();
    }
}