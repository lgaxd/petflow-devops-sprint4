package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.config.CacheConfig;
import br.com.petflow.petflow_api.dto.ClinicRequestDTO;
import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regressão: a chave do cache de listagens precisa considerar a ordenação, senão
 * uma segunda chamada com outro {@code sortBy/direction} devolveria a lista antiga.
 */
@SpringJUnitConfig({CacheConfig.class, ClinicService.class})
class CachingBehaviourTest {

    @MockitoBean
    private ClinicRepository clinicRepository;

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        clearInvocations(clinicRepository);
        Page<ClinicResponseDTO> empty = new PageImpl<>(List.of());
        when(clinicRepository.findAllProjected(any())).thenReturn(empty);
    }

    @Test
    void sameQueryIsServedFromCache() {
        var pageable = PageRequest.of(0, 10, Sort.by("name"));

        clinicService.findAll(null, pageable);
        clinicService.findAll(null, pageable);

        verify(clinicRepository, times(1)).findAllProjected(pageable);
    }

    @Test
    void differentSortingIsNotServedFromAnotherSortsCache() {
        var ascending = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));
        var descending = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));

        clinicService.findAll(null, ascending);
        clinicService.findAll(null, descending);

        verify(clinicRepository).findAllProjected(ascending);
        verify(clinicRepository).findAllProjected(descending);
    }

    @Test
    void writesEvictTheListingCache() {
        var pageable = PageRequest.of(0, 10, Sort.by("name"));
        when(clinicRepository.existsByCnpj(any())).thenReturn(false);
        when(clinicRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        clinicService.findAll(null, pageable);
        clinicService.create(ClinicRequestDTO.builder().name("Nova").cnpj("12.345.678/0001-90").build());
        clinicService.findAll(null, pageable);

        verify(clinicRepository, times(2)).findAllProjected(pageable);
    }
}
