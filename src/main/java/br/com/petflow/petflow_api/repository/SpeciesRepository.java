package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Species, Long> {
}
