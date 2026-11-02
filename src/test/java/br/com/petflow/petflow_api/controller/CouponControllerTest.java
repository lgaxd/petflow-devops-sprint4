package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.CouponResponseDTO;
import br.com.petflow.petflow_api.enums.CouponStatus;
import br.com.petflow.petflow_api.exception.BusinessRuleException;
import br.com.petflow.petflow_api.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest extends ControllerTestSupport {

    private static final String BODY = """
            {"code":"PET10","status":"DISPONIVEL","expirationDate":"2099-01-01","templateId":1}""";

    @MockitoBean
    private CouponService couponService;

    private final CouponResponseDTO coupon = CouponResponseDTO.builder()
            .id(3L).code("PET10").status(CouponStatus.DISPONIVEL).templateId(1L).build();

    @Test
    void createReturns201ForAdminAnd403ForTutor() throws Exception {
        when(couponService.create(any())).thenReturn(coupon);

        mvc.perform(json(post("/coupons"), adminToken(), BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("PET10"));
        mvc.perform(json(post("/coupons"), tutorToken(), BODY)).andExpect(status().isForbidden());
    }

    @Test
    void createRejectsPastExpirationDate() throws Exception {
        mvc.perform(json(post("/coupons"), adminToken(),
                        "{\"code\":\"X\",\"status\":\"DISPONIVEL\",\"expirationDate\":\"2000-01-01\",\"templateId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.expirationDate").exists());
    }

    @Test
    void listFiltersByStatus() throws Exception {
        when(couponService.findAll(eq("DISPONIVEL"), any())).thenReturn(page(coupon));

        mvc.perform(get("/coupons").header("Authorization", tutorToken()).param("status", "DISPONIVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DISPONIVEL"));
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        when(couponService.findAll(eq("XYZ"), any()))
                .thenThrow(new IllegalArgumentException("Status inválido. Valores permitidos: DISPONIVEL"));

        mvc.perform(get("/coupons").header("Authorization", tutorToken()).param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status inválido. Valores permitidos: DISPONIVEL"));
    }

    @Test
    void findByIdAndDelete() throws Exception {
        when(couponService.findById(3L)).thenReturn(coupon);

        mvc.perform(get("/coupons/3").header("Authorization", tutorToken())).andExpect(status().isOk());
        mvc.perform(delete("/coupons/3").header("Authorization", adminToken())).andExpect(status().isNoContent());
        mvc.perform(delete("/coupons/3").header("Authorization", tutorToken())).andExpect(status().isForbidden());
    }

    @Test
    void updateStatusMapsBusinessRuleTo422() throws Exception {
        when(couponService.updateStatus(3L, "UTILIZADO")).thenReturn(coupon);
        when(couponService.updateStatus(3L, "DISPONIVEL"))
                .thenThrow(new BusinessRuleException("transição inválida", "INVALID_COUPON_STATUS_TRANSITION"));

        mvc.perform(put("/coupons/3/status").header("Authorization", adminToken()).param("status", "UTILIZADO"))
                .andExpect(status().isOk());
        mvc.perform(put("/coupons/3/status").header("Authorization", adminToken()).param("status", "DISPONIVEL"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_COUPON_STATUS_TRANSITION"));
        mvc.perform(put("/coupons/3/status").header("Authorization", tutorToken()).param("status", "UTILIZADO"))
                .andExpect(status().isForbidden());
    }
}
