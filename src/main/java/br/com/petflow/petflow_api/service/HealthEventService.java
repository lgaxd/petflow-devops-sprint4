package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.enums.EnumParser;
import br.com.petflow.petflow_api.dto.HealthEventRequestDTO;
import br.com.petflow.petflow_api.dto.HealthEventResponseDTO;
import br.com.petflow.petflow_api.entity.*;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.exception.InvalidStatusTransitionException;
import br.com.petflow.petflow_api.repository.*;
import br.com.petflow.petflow_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class HealthEventService {

    private final HealthEventRepository healthEventRepository;
    private final PetRepository petRepository;
    private final ClinicRepository clinicRepository;
    private final EventTypeRepository eventTypeRepository;
    private final RewardPointService rewardPointService;

    @Transactional
    public HealthEventResponseDTO create(HealthEventRequestDTO request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new EntityNotFoundException("Pet", request.petId()));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(pet.getTutor().getId());
        }

        HealthEventStatus status = EnumParser.parseOrDefault(HealthEventStatus.class, request.status(), HealthEventStatus.AGENDADO);
        validateEventDate(request.eventDate(), status);

        Clinic clinic = null;
        if (request.clinicId() != null) {
            clinic = clinicRepository.findById(request.clinicId())
                    .orElseThrow(() -> new EntityNotFoundException("Clínica", request.clinicId()));
        }

        EventType eventType = eventTypeRepository.findById(request.eventTypeId())
                .orElseThrow(() -> new EntityNotFoundException("EventType", request.eventTypeId()));

        HealthEvent healthEvent = HealthEvent.builder()
                .description(request.description())
                .eventDate(request.eventDate())
                .status(status)
                .eventType(eventType)
                .pet(pet)
                .clinic(clinic)
                .build();

        healthEvent = healthEventRepository.save(healthEvent);

        // Se o evento já for criado como REALIZADO, conceder pontos
        if (status == HealthEventStatus.REALIZADO) {
            grantPointsForEvent(pet.getTutor(), healthEvent);
        }

        return toResponseDTO(healthEvent);
    }

    @Transactional(readOnly = true)
    public HealthEventResponseDTO findById(Long id) {
        HealthEvent healthEvent = healthEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento de Saúde", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(healthEvent.getPet().getTutor().getId());
        }

        return toResponseDTO(healthEvent);
    }

    @Transactional(readOnly = true)
    public Page<HealthEventResponseDTO> findAll(Long petId, String status, Pageable pageable) {
        HealthEventStatus healthStatus = EnumParser.parseOrNull(HealthEventStatus.class, status);

        Long currentTutorId = SecurityUtils.isAdmin() ? null : SecurityUtils.getCurrentTutorId();
        if (petId != null) {
            if (currentTutorId != null) {
                Pet pet = petRepository.findById(petId)
                        .orElseThrow(() -> new EntityNotFoundException("Pet", petId));
                if (!currentTutorId.equals(pet.getTutor().getId())) {
                    throw new AccessDeniedException("Você não tem permissão para visualizar eventos de outro tutor.");
                }
            }
            return healthStatus == null
                    ? healthEventRepository.findByPetIdProjected(petId, pageable)
                    : healthEventRepository.findByPetIdAndStatusProjected(petId, healthStatus, pageable);
        }
        if (currentTutorId != null) {
            return healthStatus == null
                    ? healthEventRepository.findByTutorIdProjected(currentTutorId, pageable)
                    : healthEventRepository.findByTutorIdAndStatusProjected(currentTutorId, healthStatus, pageable);
        }
        if (healthStatus != null) {
            return healthEventRepository.findByStatusProjected(healthStatus, pageable);
        }
        return healthEventRepository.findAllProjected(pageable);
    }

    @Transactional
    public HealthEventResponseDTO update(Long id, HealthEventRequestDTO request) {
        HealthEvent healthEvent = healthEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento de Saúde", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(healthEvent.getPet().getTutor().getId());
        }

        HealthEventStatus oldStatus = healthEvent.getStatus();
        HealthEventStatus newStatus = oldStatus;

        if (request.status() != null && !request.status().isBlank()) {
            newStatus = EnumParser.parse(HealthEventStatus.class, request.status());
            if (oldStatus != newStatus) {
                validateStatusTransition(oldStatus, newStatus);
                healthEvent.setStatus(newStatus);
            }
        }

        validateEventDate(request.eventDate(), newStatus);

        healthEvent.setDescription(request.description());
        healthEvent.setEventDate(request.eventDate());

        if (request.eventTypeId() != null) {
            EventType eventType = eventTypeRepository.findById(request.eventTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("EventType", request.eventTypeId()));
            healthEvent.setEventType(eventType);
        }

        if (request.clinicId() != null) {
            Clinic clinic = clinicRepository.findById(request.clinicId())
                    .orElseThrow(() -> new EntityNotFoundException("Clínica", request.clinicId()));
            healthEvent.setClinic(clinic);
        }

        // A procedure PL/SQL lê o status direto do banco: a alteração precisa estar
        // visível na transação (flush) antes de conceder os pontos.
        healthEvent = healthEventRepository.saveAndFlush(healthEvent);

        // Se o status mudou para REALIZADO, conceder pontos (evitar duplicidade)
        if (oldStatus != HealthEventStatus.REALIZADO && newStatus == HealthEventStatus.REALIZADO) {
            grantPointsForEvent(healthEvent.getPet().getTutor(), healthEvent);
        }

        return toResponseDTO(healthEvent);
    }

    @Transactional
    public void delete(Long id) {
        HealthEvent healthEvent = healthEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento de Saúde", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(healthEvent.getPet().getTutor().getId());
        }

        healthEventRepository.delete(healthEvent);
    }

    private void grantPointsForEvent(Tutor tutor, HealthEvent event) {
        rewardPointService.grantPoints(
                tutor.getId(), "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", event.getId());
    }

    // Agendado e cancelado podem ter data futura; um evento já realizado não.
    private void validateEventDate(LocalDate eventDate, HealthEventStatus status) {
        if (status == HealthEventStatus.REALIZADO && eventDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException(
                    "Um evento realizado não pode ter data futura", "FUTURE_DATE_NOT_ALLOWED");
        }
    }

    private void validateStatusTransition(HealthEventStatus currentStatus, HealthEventStatus newStatus) {
        if (currentStatus == newStatus) return;
        boolean isValid = switch (currentStatus) {
            case AGENDADO -> newStatus == HealthEventStatus.REALIZADO || newStatus == HealthEventStatus.CANCELADO;
            case REALIZADO, CANCELADO -> false;
        };
        if (!isValid) {
            throw new InvalidStatusTransitionException("HealthEvent", currentStatus.name(), newStatus.name());
        }
    }

    private HealthEventResponseDTO toResponseDTO(HealthEvent healthEvent) {
        return HealthEventResponseDTO.builder()
                .id(healthEvent.getId())
                .description(healthEvent.getDescription())
                .eventDate(healthEvent.getEventDate())
                .status(healthEvent.getStatus())
                .createdAt(healthEvent.getCreatedAt())
                .petId(healthEvent.getPet().getId())
                .petName(healthEvent.getPet().getName())
                .eventTypeId(healthEvent.getEventType() != null ? healthEvent.getEventType().getId() : null)
                .clinicId(healthEvent.getClinic() != null ? healthEvent.getClinic().getId() : null)
                .clinicName(healthEvent.getClinic() != null ? healthEvent.getClinic().getName() : null)
                .build();
    }
}