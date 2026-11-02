package br.com.petflow.petflow_api.repository;

import br.com.petflow.petflow_api.entity.RewardAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardActionRepository extends JpaRepository<RewardAction, Long> {
    Optional<RewardAction> findByName(String name);
    Optional<RewardAction> findByNameIgnoreCase(String name);
}