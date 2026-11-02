package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.CouponCatalogDTO;
import br.com.petflow.petflow_api.dto.PetRiskDTO;
import br.com.petflow.petflow_api.dto.PointHistoryDTO;
import br.com.petflow.petflow_api.dto.RedeemResponseDTO;
import br.com.petflow.petflow_api.dto.TutorPointsDTO;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.exception.ExpiredCouponException;
import br.com.petflow.petflow_api.exception.InsufficientPointsException;
import br.com.petflow.petflow_api.service.GamificationService;
import br.com.petflow.petflow_api.service.RedeemService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RedeemController.class, GamificationController.class})
class RedeemAndGamificationControllerTest extends ControllerTestSupport {

    @MockitoBean
    private RedeemService redeemService;

    @MockitoBean
    private GamificationService gamificationService;

    private final RedeemResponseDTO redeem = RedeemResponseDTO.builder()
            .id(40L).pointsUsed(100).tutorId(TUTOR_ID).couponId(3L).couponCode("PET10").build();

    @Test
    void listRedeemsFiltersByTutor() throws Exception {
        when(redeemService.findAll(eq(TUTOR_ID), any())).thenReturn(page(redeem));

        mvc.perform(get("/redeems").header("Authorization", tutorToken()).param("tutorId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponCode").value("PET10"));
        mvc.perform(get("/redeems")).andExpect(status().isUnauthorized());
    }

    @Test
    void findRedeemByIdAndNotFound() throws Exception {
        when(redeemService.findById(40L)).thenReturn(redeem);
        when(redeemService.findById(404L)).thenThrow(new EntityNotFoundException("Resgate", 404L));

        mvc.perform(get("/redeems/40").header("Authorization", tutorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pointsUsed").value(100));
        mvc.perform(get("/redeems/404").header("Authorization", tutorToken())).andExpect(status().isNotFound());
    }

    @Test
    void pointsAreReadForTheAuthenticatedTutor() throws Exception {
        when(gamificationService.getTutorPoints(TUTOR_ID)).thenReturn(TutorPointsDTO.builder()
                .tutorId(TUTOR_ID).tutorName("Tutor").totalPoints(120)
                .history(List.of(PointHistoryDTO.builder().id(1L).points(5).reason("CADASTRO_PET").build())).build());

        mvc.perform(get("/gamification/points").header("Authorization", tutorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(120))
                .andExpect(jsonPath("$.history[0].reason").value("CADASTRO_PET"));
        mvc.perform(get("/gamification/points")).andExpect(status().isUnauthorized());
    }

    @Test
    void petRiskIsReturnedOr404() throws Exception {
        when(gamificationService.getPetRisk(10L)).thenReturn(PetRiskDTO.builder()
                .petId(10L).petName("Rex").score(20).riskLevel("BAIXO").build());
        when(gamificationService.getPetRisk(404L)).thenThrow(new EntityNotFoundException("Pet", 404L));

        mvc.perform(get("/gamification/pets/10/risk").header("Authorization", tutorToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.riskLevel").value("BAIXO"));
        mvc.perform(get("/gamification/pets/404/risk").header("Authorization", tutorToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void availableCouponsArePaged() throws Exception {
        when(gamificationService.getAvailableCoupons(any())).thenReturn(page(CouponCatalogDTO.builder()
                .id(3L).code("PET10").pointsRequired(100).available(true).build()));

        mvc.perform(get("/gamification/coupons/available").header("Authorization", tutorToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].pointsRequired").value(100));
        mvc.perform(get("/gamification/coupons/available").header("Authorization", tutorToken()).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void redeemSucceedsForTheAuthenticatedTutor() throws Exception {
        when(gamificationService.redeemCoupon(TUTOR_ID, 3L)).thenReturn(redeem);

        mvc.perform(json(post("/gamification/redeem"), tutorToken(), "{\"couponId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40));
    }

    @Test
    void redeemValidatesBody() throws Exception {
        mvc.perform(json(post("/gamification/redeem"), tutorToken(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.couponId").exists());
    }

    @Test
    void redeemMapsBusinessErrorsTo422WithCodes() throws Exception {
        when(gamificationService.redeemCoupon(TUTOR_ID, 1L)).thenThrow(new InsufficientPointsException(10, 100));
        when(gamificationService.redeemCoupon(TUTOR_ID, 2L))
                .thenThrow(new ExpiredCouponException("OLD", LocalDate.of(2020, 1, 1)));

        mvc.perform(json(post("/gamification/redeem"), tutorToken(), "{\"couponId\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_POINTS"))
                .andExpect(jsonPath("$.details.availablePoints").value(10))
                .andExpect(jsonPath("$.details.requiredPoints").value(100));
        mvc.perform(json(post("/gamification/redeem"), tutorToken(), "{\"couponId\":2}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("EXPIRED_COUPON"));
    }
}
