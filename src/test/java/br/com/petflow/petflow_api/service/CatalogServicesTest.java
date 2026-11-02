package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.ClinicRequestDTO;
import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.dto.PlanRequestDTO;
import br.com.petflow.petflow_api.entity.Clinic;
import br.com.petflow.petflow_api.entity.Plan;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import br.com.petflow.petflow_api.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServicesTest {

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private ClinicService clinicService;

    @InjectMocks
    private PlanService planService;

    private final Clinic clinic = Clinic.builder().id(1L).name("PetCare").cnpj("12.345.678/0001-90").build();

    private ClinicRequestDTO clinicRequest(String cnpj) {
        return ClinicRequestDTO.builder().name("PetCare 2").address("Rua").phone("11").cnpj(cnpj).build();
    }

    private PlanRequestDTO planRequest() {
        return PlanRequestDTO.builder().name("Premium").description("d").price(new BigDecimal("89.90"))
                .durationDays(365).pointsPerEvent(5).clinicId(1L).build();
    }

    @Test
    void clinicCreateRejectsDuplicateCnpj() {
        when(clinicRepository.existsByCnpj("12.345.678/0001-90")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> clinicService.create(clinicRequest("12.345.678/0001-90")));
        verify(clinicRepository, never()).save(any());
    }

    @Test
    void clinicUpdateAllowsKeepingTheSameCnpjButNotStealingAnotherOne() {
        when(clinicRepository.findById(1L)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any(Clinic.class))).thenAnswer(call -> call.getArgument(0));

        ClinicResponseDTO updated = clinicService.update(1L, clinicRequest("12.345.678/0001-90"));
        assertEquals("PetCare 2", updated.name());

        when(clinicRepository.existsByCnpj("99.999.999/0001-99")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> clinicService.update(1L, clinicRequest("99.999.999/0001-99")));
    }

    @Test
    void clinicLookupsFailWithNotFound() {
        when(clinicRepository.findById(9L)).thenReturn(Optional.empty());
        when(clinicRepository.existsById(9L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> clinicService.findById(9L));
        assertThrows(EntityNotFoundException.class, () -> clinicService.update(9L, clinicRequest("x")));
        assertThrows(EntityNotFoundException.class, () -> clinicService.delete(9L));
    }

    @Test
    void planCreateLinksTheClinic() {
        when(clinicRepository.findById(1L)).thenReturn(Optional.of(clinic));
        when(planRepository.save(any(Plan.class))).thenAnswer(call -> call.getArgument(0));

        var response = planService.create(planRequest());

        assertEquals("Premium", response.name());
        assertEquals(1L, response.clinicId());
    }

    @Test
    void planCreateFailsWhenClinicIsMissing() {
        when(clinicRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> planService.create(planRequest()));
        verify(planRepository, never()).save(any());
    }

    @Test
    void planFindUpdateAndDeleteFailWhenPlanIsMissing() {
        when(planRepository.findById(9L)).thenReturn(Optional.empty());
        when(planRepository.existsById(9L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> planService.findById(9L));
        assertThrows(EntityNotFoundException.class, () -> planService.update(9L, planRequest()));
        assertThrows(EntityNotFoundException.class, () -> planService.delete(9L));
    }
}
