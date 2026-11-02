package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.CouponResponseDTO;
import br.com.petflow.petflow_api.entity.Coupon;
import br.com.petflow.petflow_api.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.CouponResponseDTO(
                c.id, c.code, c.status, c.expirationDate, c.template.id, c.createdAt
            )
            FROM Coupon c
            """)
    Page<CouponResponseDTO> findAllProjected(Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.CouponResponseDTO(
                c.id, c.code, c.status, c.expirationDate, c.template.id, c.createdAt
            )
            FROM Coupon c
            WHERE c.status = :status
            """)
    Page<CouponResponseDTO> findByStatus(@Param("status") CouponStatus status, Pageable pageable);

    /** Leitura com SELECT ... FOR UPDATE: serializa concessões e resgates concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
