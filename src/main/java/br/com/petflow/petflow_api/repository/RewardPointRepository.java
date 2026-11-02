package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.entity.RewardPoint;
import br.com.petflow.petflow_api.dto.PointHistoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardPointRepository extends JpaRepository<RewardPoint, Long> {
    
    @Query("SELECT SUM(rp.points) FROM RewardPoint rp WHERE rp.tutor.id = :tutorId")
    Integer sumPointsByTutorId(@Param("tutorId") Long tutorId);
    
    @Query("""
        SELECT new br.com.petflow.petflow_api.dto.PointHistoryDTO(
            rp.id, rp.points, ra.description, rp.referenceType, rp.referenceId, rp.createdAt
        )
        FROM RewardPoint rp
        JOIN rp.rewardAction ra
        WHERE rp.tutor.id = :tutorId
        ORDER BY rp.createdAt DESC
    """)
    Page<PointHistoryDTO> findHistoryByTutorId(@Param("tutorId") Long tutorId, Pageable pageable);

    boolean existsByRewardAction_IdAndReferenceTypeAndReferenceId(Long rewardActionId, String referenceType, Long referenceId);
}