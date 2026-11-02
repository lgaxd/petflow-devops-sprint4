package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.RedeemResponseDTO;
import br.com.petflow.petflow_api.entity.Redeem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RedeemRepository extends JpaRepository<Redeem, Long> {

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.RedeemResponseDTO(
                r.id, r.pointsUsed, r.createdAt,
                r.tutor.id, r.tutor.name,
                r.coupon.id, r.coupon.code
            )
            FROM Redeem r
            WHERE r.tutor.id = :tutorId
            """)
    Page<RedeemResponseDTO> findByTutorId(@Param("tutorId") Long tutorId, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.RedeemResponseDTO(
                r.id, r.pointsUsed, r.createdAt,
                r.tutor.id, r.tutor.name,
                r.coupon.id, r.coupon.code
            )
            FROM Redeem r
            """)
    Page<RedeemResponseDTO> findAllProjected(Pageable pageable);
}