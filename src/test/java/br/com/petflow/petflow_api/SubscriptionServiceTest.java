package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.SubscriptionStatus;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.PlanRepository;
import br.com.petflow.petflow_api.repository.SubscriptionRepository;
import br.com.petflow.petflow_api.service.RewardPointService;
import br.com.petflow.petflow_api.service.SubscriptionService;
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

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tutorStatusFilterUsesTutorScopedQuery() {
        Tutor tutor = Tutor.builder().id(1L).role(UserRole.TUTOR).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
        var pageable = PageRequest.of(0, 10);
        when(subscriptionRepository.findByTutorIdAndStatusProjected(1L, SubscriptionStatus.ATIVO, pageable))
                .thenReturn(Page.empty(pageable));

        subscriptionService.findAll(null, "ATIVO", pageable);

        verify(subscriptionRepository).findByTutorIdAndStatusProjected(1L, SubscriptionStatus.ATIVO, pageable);
    }
}