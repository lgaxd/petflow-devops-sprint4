package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.entity.CouponTemplate;
import br.com.petflow.petflow_api.dto.CouponCatalogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {
    
    @Query("""
        SELECT new br.com.petflow.petflow_api.dto.CouponCatalogDTO(
            c.id, c.code, ct.title, ct.pointsRequired,
            ct.discountType, ct.discountValue, c.expirationDate,
            CASE WHEN c.status = 'DISPONIVEL' AND (c.expirationDate IS NULL OR c.expirationDate >= CURRENT_DATE) THEN true ELSE false END
        )
        FROM CouponTemplate ct
        JOIN ct.coupons c
        WHERE c.status = 'DISPONIVEL'
        AND (c.expirationDate IS NULL OR c.expirationDate >= CURRENT_DATE)
    """)
    Page<CouponCatalogDTO> findAvailableCoupons(Pageable pageable);
}