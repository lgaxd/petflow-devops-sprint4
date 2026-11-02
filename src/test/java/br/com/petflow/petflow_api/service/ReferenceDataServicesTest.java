package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.EventTypeResponseDTO;
import br.com.petflow.petflow_api.dto.SpeciesResponseDTO;
import br.com.petflow.petflow_api.entity.EventType;
import br.com.petflow.petflow_api.entity.Species;
import br.com.petflow.petflow_api.repository.EventTypeRepository;
import br.com.petflow.petflow_api.repository.SpeciesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServicesTest {

    @Mock
    private SpeciesRepository speciesRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @InjectMocks
    private SpeciesService speciesService;

    @InjectMocks
    private EventTypeService eventTypeService;

    @Test
    void speciesAreMappedAndOrderedById() {
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        when(speciesRepository.findAll(sort.capture())).thenReturn(List.of(
                Species.builder().id(1L).name("Cachorro").description("Canino doméstico").build(),
                Species.builder().id(2L).name("Gato").build()));

        List<SpeciesResponseDTO> result = speciesService.findAll();

        assertEquals(2, result.size());
        assertEquals("Cachorro", result.get(0).name());
        assertEquals("Canino doméstico", result.get(0).description());
        assertEquals(Sort.by("id"), sort.getValue());
    }

    @Test
    void eventTypesAreMappedAndOrderedById() {
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        when(eventTypeRepository.findAll(sort.capture())).thenReturn(List.of(
                EventType.builder().id(1L).name("Vacinação").pointsReward(50).category("PREVENTIVO").build()));

        List<EventTypeResponseDTO> result = eventTypeService.findAll();

        assertEquals(1, result.size());
        assertEquals(50, result.get(0).pointsReward());
        assertEquals("PREVENTIVO", result.get(0).category());
        assertEquals(Sort.by("id"), sort.getValue());
    }

    @Test
    void emptyTablesReturnEmptyLists() {
        when(speciesRepository.findAll(Sort.by("id"))).thenReturn(List.of());

        assertTrue(speciesService.findAll().isEmpty());
        verify(speciesRepository).findAll(Sort.by("id"));
    }
}
