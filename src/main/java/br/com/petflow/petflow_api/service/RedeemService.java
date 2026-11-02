package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.RedeemResponseDTO;
import br.com.petflow.petflow_api.entity.Redeem;
import br.com.petflow.petflow_api.exception.*;
import br.com.petflow.petflow_api.repository.RedeemRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RedeemService {

    private final RedeemRepository redeemRepository;
    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public RedeemResponseDTO findById(Long id) {
        Redeem redeem = redeemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Resgate", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(redeem.getTutor().getId());
        }

        return toResponseDTO(redeem);
    }

    @Transactional(readOnly = true)
    public Page<RedeemResponseDTO> findAll(Long tutorId, Pageable pageable) {
        if (!SecurityUtils.isAdmin()) {
            Long currentTutorId = SecurityUtils.getCurrentTutorId();
            if (tutorId != null && !currentTutorId.equals(tutorId)) {
                throw new AccessDeniedException("Você não tem permissão para visualizar resgates de outro tutor.");
            }
            tutorId = currentTutorId;
        }

        if (tutorId != null) {
            if (!tutorRepository.existsById(tutorId)) {
                throw new EntityNotFoundException("Tutor", tutorId);
            }
            return redeemRepository.findByTutorId(tutorId, pageable);
        }
        return redeemRepository.findAllProjected(pageable);
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