package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.SubscriptionRequestDTO;
import br.com.petflow.petflow_api.dto.SubscriptionResponseDTO;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Plan;
import br.com.petflow.petflow_api.entity.Subscription;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.SubscriptionStatus;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.exception.InvalidStatusTransitionException;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.PlanRepository;
import br.com.petflow.petflow_api.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCreationTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RewardPointService rewardPointService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private final Tutor tutor = Tutor.builder().id(1L).role(UserRole.TUTOR).build();
    private final Pet pet = Pet.builder().id(10L).name("Rex").tutor(tutor).build();
    private final Plan plan = Plan.builder().id(2L).name("Premium").durationDays(30).build();

    @BeforeEach
    void authenticateAsOwner() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenPetAndPlan() {
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
    }

    private Subscription subscription(SubscriptionStatus status) {
        return Subscription.builder().id(30L).status(status).pet(pet).plan(plan).startDate(START).build();
    }

    @Test
    void endDateDefaultsToPlanDurationAndPointsAreGranted() {
        givenPetAndPlan();
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(call -> {
            Subscription saved = call.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        SubscriptionResponseDTO response = subscriptionService.create(
                new SubscriptionRequestDTO(START, null, null, 10L, 2L));

        assertEquals(START.plusDays(30), response.endDate());
        assertEquals(SubscriptionStatus.ATIVO, response.status());
        verify(rewardPointService).grantPoints(1L, "ASSINATURA_ATIVA", "SUBSCRIPTION", 30L);
    }

    @Test
    void endDateBeforeStartDateIsRejectedBeforeAnythingIsSaved() {
        givenPetAndPlan();

        BusinessRuleException error = assertThrows(BusinessRuleException.class, () ->
                subscriptionService.create(new SubscriptionRequestDTO(START, START.minusDays(1), "ATIVO", 10L, 2L)));

        assertEquals("INVALID_DATE_RANGE", error.getCode());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void nonActiveSubscriptionDoesNotGrantPoints() {
        givenPetAndPlan();
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));

        subscriptionService.create(new SubscriptionRequestDTO(START, START.plusDays(5), "ENCERRADO", 10L, 2L));

        verify(rewardPointService, never()).grantPoints(any(), any(), any(), any());
    }

    @Test
    void activeSubscriptionCanBeCancelled() {
        when(subscriptionRepository.findById(30L)).thenReturn(Optional.of(subscription(SubscriptionStatus.ATIVO)));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(SubscriptionStatus.CANCELADO, subscriptionService.updateStatus(30L, "cancelado").status());
    }

    @Test
    void closedSubscriptionCannotBeReactivated() {
        when(subscriptionRepository.findById(30L)).thenReturn(Optional.of(subscription(SubscriptionStatus.CANCELADO)));

        assertThrows(InvalidStatusTransitionException.class, () -> subscriptionService.updateStatus(30L, "ATIVO"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void activeSubscriptionCannotBecomeExpiredManually() {
        when(subscriptionRepository.findById(30L)).thenReturn(Optional.of(subscription(SubscriptionStatus.ATIVO)));

        assertThrows(InvalidStatusTransitionException.class, () -> subscriptionService.updateStatus(30L, "EXPIRADO"));
    }

    @Test
    void expirationJobMarksOverdueSubscriptionsAsExpired() {
        Subscription overdue = subscription(SubscriptionStatus.ATIVO);
        when(subscriptionRepository.findExpiredSubscriptions(eq(SubscriptionStatus.ATIVO), any(LocalDate.class)))
                .thenReturn(List.of(overdue));

        subscriptionService.expireSubscriptions();

        assertEquals(SubscriptionStatus.EXPIRADO, overdue.getStatus());
        verify(subscriptionRepository).save(overdue);
    }
}
