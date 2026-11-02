package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.PlanResponseDTO;
import br.com.petflow.petflow_api.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PlanResponseDTO(
                p.id, p.name, p.description, p.price, p.durationDays, p.pointsPerEvent,
                p.clinic.id, p.clinic.name
            )
            FROM Plan p
            WHERE p.clinic.id = :clinicId
            """)
    Page<PlanResponseDTO> findByClinicIdProjected(@Param("clinicId") Long clinicId, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PlanResponseDTO(
                p.id, p.name, p.description, p.price, p.durationDays, p.pointsPerEvent,
                p.clinic.id, p.clinic.name
            )
            FROM Plan p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<PlanResponseDTO> findByNameProjected(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PlanResponseDTO(
                p.id, p.name, p.description, p.price, p.durationDays, p.pointsPerEvent,
                p.clinic.id, p.clinic.name
            )
            FROM Plan p
            """)
    Page<PlanResponseDTO> findAllProjected(Pageable pageable);
}