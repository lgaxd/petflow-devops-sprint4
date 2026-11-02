package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.PlanRequestDTO;
import br.com.petflow.petflow_api.dto.PlanResponseDTO;
import br.com.petflow.petflow_api.entity.Clinic;
import br.com.petflow.petflow_api.entity.Plan;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import br.com.petflow.petflow_api.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ClinicRepository clinicRepository;

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public PlanResponseDTO create(PlanRequestDTO request) {
        Clinic clinic = clinicRepository.findById(request.clinicId())
            .orElseThrow(() -> new EntityNotFoundException("Clínica", request.clinicId()));

        Plan plan = Plan.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .durationDays(request.durationDays())
                .pointsPerEvent(request.pointsPerEvent())
                .clinic(clinic)
                .build();

        plan = planRepository.save(plan);
        return toResponseDTO(plan);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "plans", key = "#id")
    public PlanResponseDTO findById(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plano", id));
        return toResponseDTO(plan);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "plans", key = "{#clinicId, #pageable}")
    public Page<PlanResponseDTO> findAll(Long clinicId, Pageable pageable) {
        if (clinicId != null) {
            return planRepository.findByClinicIdProjected(clinicId, pageable);
        }
        return planRepository.findAllProjected(pageable);
    }

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public PlanResponseDTO update(Long id, PlanRequestDTO request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plano", id));

        if (!plan.getClinic().getId().equals(request.clinicId())) {
            Clinic clinic = clinicRepository.findById(request.clinicId())
                .orElseThrow(() -> new EntityNotFoundException("Clínica", request.clinicId()));
            plan.setClinic(clinic);
        }

        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setDurationDays(request.durationDays());
        plan.setPointsPerEvent(request.pointsPerEvent());

        plan = planRepository.save(plan);
        return toResponseDTO(plan);
    }

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public void delete(Long id) {
        if (!planRepository.existsById(id)) {
            throw new EntityNotFoundException("Plano", id);
        }
        planRepository.deleteById(id);
    }

    private PlanResponseDTO toResponseDTO(Plan plan) {
        return PlanResponseDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .pointsPerEvent(plan.getPointsPerEvent())
                .clinicId(plan.getClinic().getId())
                .clinicName(plan.getClinic().getName())
                .build();
    }
}