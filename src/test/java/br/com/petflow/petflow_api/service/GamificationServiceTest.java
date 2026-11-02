package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.PetRiskDTO;
import br.com.petflow.petflow_api.dto.RedeemResponseDTO;
import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Redeem;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.repository.CouponTemplateRepository;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.RedeemRepository;
import br.com.petflow.petflow_api.repository.RewardPointRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.service.RewardPointService.RedeemResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private RewardPointRepository rewardPointRepository;

    @Mock
    private CouponTemplateRepository couponTemplateRepository;

    @Mock
    private RedeemRepository redeemRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private RewardPointService rewardPointService;

    @InjectMocks
    private GamificationService gamificationService;

    private final Tutor tutor = Tutor.builder().id(1L).name("Tutor").role(UserRole.TUTOR).build();

    @BeforeEach
    void authenticateAsTutor() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                tutor, null, List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private PetRiskDTO riskFor(LocalDate birthDate, String weight) {
        Pet pet = Pet.builder().id(10L).name("Rex").tutor(tutor).birthDate(birthDate)
                .weight(weight == null ? null : new BigDecimal(weight)).build();
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        return gamificationService.getPetRisk(10L);
    }

    @ParameterizedTest(name = "{0} anos e {1} kg => score {2} ({3})")
    @CsvSource({
            "1,10.0,0,BAIXO",
            "6,10.0,5,BAIXO",
            "8,10.0,10,BAIXO",
            "11,10.0,20,BAIXO",
            "1,2.0,15,BAIXO",
            "1,4.0,8,BAIXO",
            "1,26.0,8,BAIXO",
            "1,31.0,15,BAIXO",
            "11,31.0,35,MEDIO"
    })
    void riskScoreCombinesAgeAndWeight(int ageYears, String weight, int expectedScore, String expectedLevel) {
        PetRiskDTO risk = riskFor(LocalDate.now().minusYears(ageYears).minusDays(1), weight);

        assertEquals(expectedScore, risk.score());
        assertEquals(expectedLevel, risk.riskLevel());
    }

    @Test
    void petWithoutBirthDateAndWeightHasNoRisk() {
        PetRiskDTO risk = riskFor(null, null);

        assertEquals(0, risk.score());
        assertEquals("Pet saudável com baixo risco", risk.riskDescription());
    }

    @Test
    void riskOfAnotherTutorsPetIsForbidden() {
        Tutor stranger = Tutor.builder().id(99L).build();
        Pet pet = Pet.builder().id(11L).tutor(stranger).build();
        when(petRepository.findById(11L)).thenReturn(Optional.of(pet));

        assertThrows(AccessDeniedException.class, () -> gamificationService.getPetRisk(11L));
    }

    @Test
    void redeemReturnsPersistedRedeem() {
        Redeem redeem = Redeem.builder().id(40L).pointsUsed(100).tutor(tutor)
                .coupon(Coupon.builder().id(3L).code("PET10").build()).build();
        when(rewardPointService.redeemCoupon(1L, 3L)).thenReturn(new RedeemResult(40L, 100));
        when(redeemRepository.findById(40L)).thenReturn(Optional.of(redeem));

        RedeemResponseDTO response = gamificationService.redeemCoupon(1L, 3L);

        assertEquals(40L, response.id());
        assertEquals("PET10", response.couponCode());
    }

    @Test
    void redeemForAnotherTutorIsForbidden() {
        assertThrows(AccessDeniedException.class, () -> gamificationService.redeemCoupon(2L, 3L));
    }
}
