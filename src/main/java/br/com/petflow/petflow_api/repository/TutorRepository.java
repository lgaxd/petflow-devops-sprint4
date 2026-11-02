package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.TutorResponseDTO;
import br.com.petflow.petflow_api.entity.Tutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TutorRepository extends JpaRepository<Tutor, Long> {

    Optional<Tutor> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.TutorResponseDTO(
                t.id, t.name, t.email, t.phone, t.createdAt
            )
            FROM Tutor t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<TutorResponseDTO> findByNameProjected(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.TutorResponseDTO(
                t.id, t.name, t.email, t.phone, t.createdAt
            )
            FROM Tutor t
            """)
    Page<TutorResponseDTO> findAllProjected(Pageable pageable);

    /** Leitura com SELECT ... FOR UPDATE: serializa concessões e resgates concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tutor t WHERE t.id = :id")
    Optional<Tutor> findByIdForUpdate(@Param("id") Long id);
}
