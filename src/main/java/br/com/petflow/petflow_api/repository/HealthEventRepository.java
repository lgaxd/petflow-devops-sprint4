package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.HealthEventResponseDTO;
import br.com.petflow.petflow_api.entity.HealthEvent;
import br.com.petflow.petflow_api.enums.HealthEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {
    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
                h.id, h.description, h.eventDate, h.status, h.createdAt,
                h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            WHERE h.pet.id = :petId
            """)
    Page<HealthEventResponseDTO> findByPetIdProjected(@Param("petId") Long petId, Pageable pageable);

        @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
            h.id, h.description, h.eventDate, h.status, h.createdAt,
            h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            WHERE h.pet.id = :petId AND h.status = :status
            """)
        Page<HealthEventResponseDTO> findByPetIdAndStatusProjected(
            @Param("petId") Long petId,
            @Param("status") HealthEventStatus status,
            Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
                h.id, h.description, h.eventDate, h.status, h.createdAt,
                h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            WHERE h.status = :status
            """)
    Page<HealthEventResponseDTO> findByStatusProjected(@Param("status") HealthEventStatus status, Pageable pageable);

        @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
            h.id, h.description, h.eventDate, h.status, h.createdAt,
            h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            WHERE h.pet.tutor.id = :tutorId
            """)
        Page<HealthEventResponseDTO> findByTutorIdProjected(@Param("tutorId") Long tutorId, Pageable pageable);

        @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
            h.id, h.description, h.eventDate, h.status, h.createdAt,
            h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            WHERE h.pet.tutor.id = :tutorId AND h.status = :status
            """)
        Page<HealthEventResponseDTO> findByTutorIdAndStatusProjected(
            @Param("tutorId") Long tutorId,
            @Param("status") HealthEventStatus status,
            Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.HealthEventResponseDTO(
                h.id, h.description, h.eventDate, h.status, h.createdAt,
                h.pet.id, h.pet.name, h.eventType.id, c.id, c.name
            )
            FROM HealthEvent h
            LEFT JOIN h.clinic c
            """)
    Page<HealthEventResponseDTO> findAllProjected(Pageable pageable);

    /** Leitura com SELECT ... FOR UPDATE: serializa concessões e resgates concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HealthEvent h WHERE h.id = :id")
    Optional<HealthEvent> findByIdForUpdate(@Param("id") Long id);
}
