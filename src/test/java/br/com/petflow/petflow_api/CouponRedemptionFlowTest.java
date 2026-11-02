package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.entity.RewardAction;
import br.com.petflow.petflow_api.entity.RewardPoint;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.CouponStatus;
import br.com.petflow.petflow_api.repository.CouponRepository;
import br.com.petflow.petflow_api.repository.RewardActionRepository;
import br.com.petflow.petflow_api.repository.RewardPointRepository;
import br.com.petflow.petflow_api.repository.RedeemRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
class CouponRedemptionFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${TEST_TUTOR_EMAIL}")
    private String testTutorEmail;

    @Value("${TEST_TUTOR_PASSWORD}")
    private String testTutorPassword;

        private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private RewardActionRepository rewardActionRepository;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private RedeemRepository redeemRepository;

    @Test
    void catalogCouponCanBeRedeemedByItsReturnedId() throws Exception {
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "email", testTutorEmail,
                    "password", testTutorPassword))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("token").asText();

        JsonNode firstCoupon = objectMapper.readTree(mockMvc.perform(get("/gamification/coupons/available")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("content").get(0);

        long couponId = firstCoupon.get("id").asLong();
        String catalogCode = firstCoupon.get("code").asText();
        Coupon catalogCoupon = couponRepository.findById(couponId).orElse(null);

        assertNotNull(catalogCoupon);
        assertEquals(CouponStatus.DISPONIVEL, catalogCoupon.getStatus());

        Tutor tutor = tutorRepository.findByEmail(testTutorEmail).orElseThrow();
        RewardAction rewardAction = rewardActionRepository.findByName("CADASTRO_PET").orElseThrow();
        RewardPoint testPoints = rewardPointRepository.save(RewardPoint.builder()
            .tutor(tutor)
            .rewardAction(rewardAction)
            .points(1000)
            .referenceType("TEST")
            .referenceId(couponId)
            .build());

        try {
            String redeemResponse = mockMvc.perform(post("/gamification/redeem")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"couponId\":" + couponId + "}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            assertEquals(catalogCode, objectMapper.readTree(redeemResponse).get("couponCode").asText());
            assertEquals(CouponStatus.RESGATADO, couponRepository.findById(couponId).orElseThrow().getStatus());
        } finally {
            rewardPointRepository.delete(testPoints);
            redeemRepository.findAll().stream()
                .filter(redeem -> redeem.getCoupon().getId().equals(couponId)
                    && redeem.getTutor().getId().equals(tutor.getId()))
                .forEach(redeem -> {
                rewardPointRepository.findAll().stream()
                    .filter(point -> "REDEEM".equals(point.getReferenceType())
                        && redeem.getId().equals(point.getReferenceId()))
                    .forEach(rewardPointRepository::delete);
                redeemRepository.delete(redeem);
                });
            couponRepository.findById(couponId).ifPresent(coupon -> {
            coupon.setStatus(CouponStatus.DISPONIVEL);
            couponRepository.save(coupon);
            });
        }
    }
}
