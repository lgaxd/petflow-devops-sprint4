package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.ClinicRequestDTO;
import br.com.petflow.petflow_api.dto.ClinicResponseDTO;
import br.com.petflow.petflow_api.entity.Clinic;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional
    @CacheEvict(value = "clinics", allEntries = true)
    public ClinicResponseDTO create(ClinicRequestDTO request) {
        if (clinicRepository.existsByCnpj(request.cnpj())) {
            throw new DuplicateResourceException("Clínica", "CNPJ", request.cnpj());
        }

        Clinic clinic = Clinic.builder()
                .name(request.name())
                .address(request.address())
                .phone(request.phone())
                .cnpj(request.cnpj())
                .build();

        clinic = clinicRepository.save(clinic);
        return toResponseDTO(clinic);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "clinics", key = "#id")
    public ClinicResponseDTO findById(Long id) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clínica", id));
        return toResponseDTO(clinic);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "clinics", key = "{#name, #pageable}")
    public Page<ClinicResponseDTO> findAll(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return clinicRepository.findByNameProjected(name, pageable);
        }
        return clinicRepository.findAllProjected(pageable);
    }

    @Transactional
    @CacheEvict(value = "clinics", allEntries = true)
    public ClinicResponseDTO update(Long id, ClinicRequestDTO request) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Clínica", id));

        if (!clinic.getCnpj().equals(request.cnpj()) &&
                clinicRepository.existsByCnpj(request.cnpj())) {
            throw new DuplicateResourceException("Clínica", "CNPJ", request.cnpj());
        }

        clinic.setName(request.name());
        clinic.setAddress(request.address());
        clinic.setPhone(request.phone());
        clinic.setCnpj(request.cnpj());

        clinic = clinicRepository.save(clinic);
        return toResponseDTO(clinic);
    }

    @Transactional
    @CacheEvict(value = "clinics", allEntries = true)
    public void delete(Long id) {
        if (!clinicRepository.existsById(id)) {
            throw new EntityNotFoundException("Clínica", id);
        }
        clinicRepository.deleteById(id);
    }

    private ClinicResponseDTO toResponseDTO(Clinic clinic) {
        return ClinicResponseDTO.builder()
                .id(clinic.getId())
                .name(clinic.getName())
                .address(clinic.getAddress())
                .phone(clinic.getPhone())
                .cnpj(clinic.getCnpj())
                .createdAt(clinic.getCreatedAt())
                .build();
    }
}