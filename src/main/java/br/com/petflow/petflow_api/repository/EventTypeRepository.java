package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, Long> {
}
