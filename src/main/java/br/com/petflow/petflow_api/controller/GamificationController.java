package br.com.petflow.petflow_api.controller;

import br.com.petflow.petflow_api.dto.*;
import br.com.petflow.petflow_api.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import br.com.petflow.petflow_api.security.AuthenticatedTutor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@RequestMapping("/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamificação", description = "Endpoints para gamificação, pontos e recompensas")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/points")
    @Operation(summary = "Buscar pontos do tutor autenticado")
    public ResponseEntity<TutorPointsDTO> getMyPoints(Authentication authentication) {
        AuthenticatedTutor userDetails = (AuthenticatedTutor) authentication.getPrincipal();
        Long tutorId = extractTutorId(userDetails);
        return ResponseEntity.ok(gamificationService.getTutorPoints(tutorId));
    }

    @GetMapping("/pets/{petId}/risk")
    @Operation(summary = "Buscar score de risco de um pet")
    public ResponseEntity<PetRiskDTO> getPetRisk(@PathVariable Long petId) {
        return ResponseEntity.ok(gamificationService.getPetRisk(petId));
    }

    @GetMapping("/coupons/available")
    @Operation(summary = "Listar cupons disponíveis para resgate")
    public ResponseEntity<Page<CouponCatalogDTO>> getAvailableCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = ApiPageable.of(page, size, "pointsRequired", "asc", Set.of("pointsRequired"));
        return ResponseEntity.ok(gamificationService.getAvailableCoupons(pageable));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Resgatar um cupom usando pontos")
    public ResponseEntity<RedeemResponseDTO> redeemCoupon(
            Authentication authentication,
            @Valid @RequestBody RedeemCouponRequestDTO request) {
        AuthenticatedTutor userDetails = (AuthenticatedTutor) authentication.getPrincipal();
        Long tutorId = extractTutorId(userDetails);
        return ResponseEntity.ok(gamificationService.redeemCoupon(tutorId, request.couponId()));
    }

    private Long extractTutorId(AuthenticatedTutor userDetails) {
        return userDetails.getId();
    }
}