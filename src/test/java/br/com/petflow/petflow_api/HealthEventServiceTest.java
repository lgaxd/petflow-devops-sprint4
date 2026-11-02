package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.dto.HealthEventRequestDTO;
import br.com.petflow.petflow_api.entity.EventType;
import br.com.petflow.petflow_api.entity.HealthEvent;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import br.com.petflow.petflow_api.repository.EventTypeRepository;
import br.com.petflow.petflow_api.repository.HealthEventRepository;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.service.HealthEventService;
import br.com.petflow.petflow_api.service.RewardPointService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthEventServiceTest {

    @Mock
    private HealthEventRepository healthEventRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private RewardPointService rewardPointService;

    @InjectMocks
    private HealthEventService healthEventService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void realizedEventUsesPointsConfiguredByItsEventType() {
        Tutor tutor = Tutor.builder().id(1L).role(UserRole.TUTOR).build();
        Pet pet = Pet.builder().id(9L).tutor(tutor).build();
        EventType eventType = EventType.builder().id(3L).name("Consulta").pointsReward(5).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
        when(petRepository.findById(9L)).thenReturn(Optional.of(pet));
        when(eventTypeRepository.findById(3L)).thenReturn(Optional.of(eventType));
        when(healthEventRepository.save(any(HealthEvent.class))).thenAnswer(invocation -> {
            HealthEvent event = invocation.getArgument(0);
            event.setId(25L);
            return event;
        });

        healthEventService.create(new HealthEventRequestDTO(
                "Check-up", LocalDate.now(), "REALIZADO", 9L, 3L, null));

        verify(rewardPointService).grantPoints(1L, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", 25L);
    }

    @Test
    void tutorStatusFilterUsesTutorScopedQuery() {
        Tutor tutor = Tutor.builder().id(1L).role(UserRole.TUTOR).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
        var pageable = PageRequest.of(0, 10);
        when(healthEventRepository.findByTutorIdAndStatusProjected(1L,
                br.com.petflow.petflow_api.enums.HealthEventStatus.REALIZADO, pageable))
                .thenReturn(Page.empty(pageable));

        healthEventService.findAll(null, "REALIZADO", pageable);

        verify(healthEventRepository).findByTutorIdAndStatusProjected(1L,
                br.com.petflow.petflow_api.enums.HealthEventStatus.REALIZADO, pageable);
    }
}