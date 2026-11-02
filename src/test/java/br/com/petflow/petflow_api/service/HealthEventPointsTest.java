package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.HealthEventRequestDTO;
import br.com.petflow.petflow_api.entity.EventType;
import br.com.petflow.petflow_api.entity.HealthEvent;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.InvalidStatusTransitionException;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import br.com.petflow.petflow_api.repository.EventTypeRepository;
import br.com.petflow.petflow_api.repository.HealthEventRepository;
import br.com.petflow.petflow_api.repository.PetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthEventPointsTest {

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

    private final Tutor tutor = Tutor.builder().id(1L).role(UserRole.TUTOR).build();
    private final Pet pet = Pet.builder().id(10L).tutor(tutor).build();

    @BeforeEach
    void authenticateAsOwner() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private HealthEvent event(HealthEventStatus status) {
        return HealthEvent.builder().id(20L).status(status).pet(pet).build();
    }

    private HealthEventRequestDTO update(String status) {
        return HealthEventRequestDTO.builder().description("d").eventDate(LocalDate.now())
                .status(status).petId(10L).eventTypeId(1L).build();
    }

    @Test
    void completingAnEventFlushesStatusBeforeTheProcedureGrantsPoints() {
        HealthEvent scheduled = event(HealthEventStatus.AGENDADO);
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(scheduled));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType()));
        when(healthEventRepository.saveAndFlush(any(HealthEvent.class))).thenAnswer(call -> call.getArgument(0));

        var response = healthEventService.update(20L, update("REALIZADO"));

        assertEquals(HealthEventStatus.REALIZADO, response.status());
        InOrder order = inOrder(healthEventRepository, rewardPointService);
        order.verify(healthEventRepository).saveAndFlush(scheduled);
        order.verify(rewardPointService).grantPoints(1L, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", 20L);
    }

    @Test
    void updatingAnAlreadyCompletedEventDoesNotGrantPointsTwice() {
        HealthEvent done = event(HealthEventStatus.REALIZADO);
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(done));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType()));
        when(healthEventRepository.saveAndFlush(any(HealthEvent.class))).thenAnswer(call -> call.getArgument(0));

        healthEventService.update(20L, update("REALIZADO"));

        verify(rewardPointService, never()).grantPoints(any(), any(), any(), any());
    }

    private HealthEventRequestDTO request(String status, LocalDate date) {
        return HealthEventRequestDTO.builder().description("d").eventDate(date)
                .status(status).petId(10L).eventTypeId(1L).build();
    }

    @Test
    void scheduledEventCanBeCreatedWithFutureDate() {
        LocalDate nextWeek = LocalDate.now().plusDays(7);
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType()));
        when(healthEventRepository.save(any(HealthEvent.class))).thenAnswer(call -> call.getArgument(0));

        var response = healthEventService.create(request("AGENDADO", nextWeek));

        assertEquals(nextWeek, response.eventDate());
        assertEquals(HealthEventStatus.AGENDADO, response.status());
        verify(rewardPointService, never()).grantPoints(any(), any(), any(), any());
    }

    @Test
    void realizedEventCannotBeCreatedWithFutureDate() {
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> healthEventService.create(request("REALIZADO", LocalDate.now().plusDays(1))));

        assertEquals("FUTURE_DATE_NOT_ALLOWED", ex.getCode());
        verify(healthEventRepository, never()).save(any());
        verify(rewardPointService, never()).grantPoints(any(), any(), any(), any());
    }

    @Test
    void scheduledEventCanBeUpdatedToAnotherFutureDate() {
        LocalDate later = LocalDate.now().plusDays(30);
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(event(HealthEventStatus.AGENDADO)));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType()));
        when(healthEventRepository.saveAndFlush(any(HealthEvent.class))).thenAnswer(call -> call.getArgument(0));

        var response = healthEventService.update(20L, request("AGENDADO", later));

        assertEquals(later, response.eventDate());
    }

    @Test
    void cancelledEventCanKeepFutureDate() {
        LocalDate later = LocalDate.now().plusDays(10);
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(event(HealthEventStatus.AGENDADO)));
        when(eventTypeRepository.findById(1L)).thenReturn(Optional.of(new EventType()));
        when(healthEventRepository.saveAndFlush(any(HealthEvent.class))).thenAnswer(call -> call.getArgument(0));

        var response = healthEventService.update(20L, request("CANCELADO", later));

        assertEquals(HealthEventStatus.CANCELADO, response.status());
    }

    @Test
    void scheduledEventCannotBeCompletedWithFutureDate() {
        HealthEvent scheduled = event(HealthEventStatus.AGENDADO);
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(scheduled));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> healthEventService.update(20L, request("REALIZADO", LocalDate.now().plusDays(3))));

        assertEquals("FUTURE_DATE_NOT_ALLOWED", ex.getCode());
        verify(healthEventRepository, never()).saveAndFlush(any());
        verify(rewardPointService, never()).grantPoints(any(), any(), any(), any());
    }

    @Test
    void completedEventsCannotGoBackToScheduled() {
        when(healthEventRepository.findById(20L)).thenReturn(Optional.of(event(HealthEventStatus.REALIZADO)));

        assertThrows(InvalidStatusTransitionException.class, () -> healthEventService.update(20L, update("AGENDADO")));
        verify(healthEventRepository, never()).saveAndFlush(any());
    }
}
