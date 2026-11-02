package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.EventTypeResponseDTO;
import br.com.petflow.petflow_api.entity.EventType;
import br.com.petflow.petflow_api.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lista de tipos de evento de saúde (tabela de domínio). Sem cache, pelo mesmo motivo de {@link SpeciesService}. */
@Service
@RequiredArgsConstructor
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;

    @Transactional(readOnly = true)
    public List<EventTypeResponseDTO> findAll() {
        return eventTypeRepository.findAll(Sort.by("id")).stream().map(this::toResponseDTO).toList();
    }

    private EventTypeResponseDTO toResponseDTO(EventType eventType) {
        return EventTypeResponseDTO.builder()
                .id(eventType.getId())
                .name(eventType.getName())
                .pointsReward(eventType.getPointsReward())
                .category(eventType.getCategory())
                .build();
    }
}
