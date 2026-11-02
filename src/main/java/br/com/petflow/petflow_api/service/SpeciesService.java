package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.SpeciesResponseDTO;
import br.com.petflow.petflow_api.entity.Species;
import br.com.petflow.petflow_api.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lista de espécies. Sem cache de propósito: a tabela é pequena e também recebe
 * inserções diretas pelos pacotes PL/SQL, que um cache na aplicação deixaria desatualizadas.
 */
@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;

    @Transactional(readOnly = true)
    public List<SpeciesResponseDTO> findAll() {
        return speciesRepository.findAll(Sort.by("id")).stream().map(this::toResponseDTO).toList();
    }

    private SpeciesResponseDTO toResponseDTO(Species species) {
        return SpeciesResponseDTO.builder()
                .id(species.getId())
                .name(species.getName())
                .description(species.getDescription())
                .build();
    }
}
