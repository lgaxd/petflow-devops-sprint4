package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PetResponseDTO(
                p.id, p.name, p.breed, p.birthDate, p.weight, p.speciesId, p.createdAt,
                p.tutor.id, p.tutor.name
            )
            FROM Pet p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<PetResponseDTO> findByNameProjected(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PetResponseDTO(
                p.id, p.name, p.breed, p.birthDate, p.weight, p.speciesId, p.createdAt,
                p.tutor.id, p.tutor.name
            )
            FROM Pet p
            WHERE p.tutor.id = :tutorId
            """)
    Page<PetResponseDTO> findByTutorIdProjected(@Param("tutorId") Long tutorId, Pageable pageable);

        @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PetResponseDTO(
            p.id, p.name, p.breed, p.birthDate, p.weight, p.speciesId, p.createdAt,
            p.tutor.id, p.tutor.name
            )
            FROM Pet p
            WHERE p.tutor.id = :tutorId
            AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
        Page<PetResponseDTO> findByTutorIdAndNameProjected(
            @Param("tutorId") Long tutorId,
            @Param("name") String name,
            Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.PetResponseDTO(
                p.id, p.name, p.breed, p.birthDate, p.weight, p.speciesId, p.createdAt,
                p.tutor.id, p.tutor.name
            )
            FROM Pet p
            """)
    Page<PetResponseDTO> findAllProjected(Pageable pageable);

    /** Leitura com SELECT ... FOR UPDATE: serializa concessões e resgates concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pet p WHERE p.id = :id")
    Optional<Pet> findByIdForUpdate(@Param("id") Long id);
}
