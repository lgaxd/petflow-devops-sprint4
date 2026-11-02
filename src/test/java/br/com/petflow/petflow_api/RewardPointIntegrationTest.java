package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.entity.HealthEvent;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import br.com.petflow.petflow_api.repository.HealthEventRepository;
import br.com.petflow.petflow_api.repository.RewardPointRepository;
import br.com.petflow.petflow_api.service.RewardPointService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Tag("integration")
class RewardPointIntegrationTest {

    @Autowired
    private RewardPointService rewardPointService;

    @Autowired
    private HealthEventRepository healthEventRepository;

    @Autowired
    private RewardPointRepository rewardPointRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void realizedEventGrantIsIdempotentAndTutorBalanceComesFromService() {
        HealthEvent event = healthEventRepository.findAll().stream()
                .filter(healthEvent -> healthEvent.getStatus() == HealthEventStatus.REALIZADO)
                .findFirst()
                .orElseThrow(() -> new AssertionError("O schema de integração precisa conter um evento REALIZADO."));
        Long tutorId = event.getPet().getTutor().getId();
        Long eventId = event.getId();
        Integer pointsBefore = rewardPointRepository.sumPointsByTutorId(tutorId);
        if (pointsBefore == null) pointsBefore = 0;
        Integer eventPointRowsBefore = jdbcTemplate.queryForObject(
                "select count(*) from REWARD_POINT where REWARD_ACTION_ID = "
                        + "(select ID from REWARD_ACTION where NAME = ?) "
                        + "and REFERENCE_TYPE = ? and REFERENCE_ID = ?",
                Integer.class,
                "EVENTO_SAUDE_REALIZADO",
                "HEALTH_EVENT",
                eventId);

        rewardPointService.grantPoints(tutorId, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", eventId);
        rewardPointService.grantPoints(tutorId, "EVENTO_SAUDE_REALIZADO", "HEALTH_EVENT", eventId);

        Integer eventPointRowsAfter = jdbcTemplate.queryForObject(
                "select count(*) from REWARD_POINT where REWARD_ACTION_ID = "
                        + "(select ID from REWARD_ACTION where NAME = ?) "
                        + "and REFERENCE_TYPE = ? and REFERENCE_ID = ?",
                Integer.class,
                "EVENTO_SAUDE_REALIZADO",
                "HEALTH_EVENT",
                eventId);
        Integer pointsAfter = rewardPointService.getTutorPoints(tutorId);

        assertTrue(eventPointRowsBefore != null && eventPointRowsBefore <= 1);
        assertEquals(Math.max(eventPointRowsBefore, 1), eventPointRowsAfter);
        assertEquals(pointsBefore + (eventPointRowsBefore == 0 ? event.getEventType().getPointsReward() : 0), pointsAfter);
    }

        @Test
        @Transactional
        void packageRejectsRewardForAnotherTutor() {
                HealthEvent event = healthEventRepository.findAll().stream()
                                .filter(healthEvent -> healthEvent.getStatus() == HealthEventStatus.REALIZADO)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("O schema de integração precisa conter um evento REALIZADO."));

                assertThrows(AccessDeniedException.class, () -> rewardPointService.grantPoints(
                                event.getPet().getTutor().getId() + 100000L,
                                "EVENTO_SAUDE_REALIZADO",
                                "HEALTH_EVENT",
                                event.getId()));
        }
}