package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.entity.Clinic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    boolean existsByCnpj(String cnpj);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.ClinicResponseDTO(
                c.id, c.name, c.address, c.phone, c.cnpj, c.createdAt
            )
            FROM Clinic c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    Page<ClinicResponseDTO> findByNameProjected(@Param("name") String name, Pageable pageable);

    @Query("""
            SELECT new br.com.petflow.petflow_api.dto.ClinicResponseDTO(
                c.id, c.name, c.address, c.phone, c.cnpj, c.createdAt
            )
            FROM Clinic c
            """)
    Page<ClinicResponseDTO> findAllProjected(Pageable pageable);
}