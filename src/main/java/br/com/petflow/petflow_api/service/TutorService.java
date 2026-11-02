package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.TutorRequestDTO;
import br.com.petflow.petflow_api.dto.TutorUpdateRequestDTO;
import br.com.petflow.petflow_api.dto.TutorResponseDTO;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class TutorService {

    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CacheEvict(value = "tutors", allEntries = true)
    public TutorResponseDTO create(TutorRequestDTO request) {
        if (tutorRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Tutor", "email", request.email());
        }

        Tutor tutor = Tutor.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.TUTOR)
                .build();

        tutor = tutorRepository.save(tutor);
        return toResponseDTO(tutor);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tutors", key = "#id")
    public TutorResponseDTO findById(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor", id));
        return toResponseDTO(tutor);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tutors", key = "{#name, #pageable}")
    public Page<TutorResponseDTO> findAll(String name, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return tutorRepository.findByNameProjected(name, pageable);
        }
        return tutorRepository.findAllProjected(pageable);
    }

    @Transactional
    @CacheEvict(value = "tutors", allEntries = true)
    public TutorResponseDTO update(Long id, TutorUpdateRequestDTO request) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tutor", id));

        if (!tutor.getEmail().equals(request.email()) &&
                tutorRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Tutor", "email", request.email());
        }

        tutor.setName(request.name());
        tutor.setEmail(request.email());
        tutor.setPhone(request.phone());

        if (request.password() != null && !request.password().isBlank()) {
            tutor.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        tutor = tutorRepository.save(tutor);
        return toResponseDTO(tutor);
    }

    @Transactional
    @CacheEvict(value = "tutors", allEntries = true)
    public void delete(Long id) {
        if (!tutorRepository.existsById(id)) {
            throw new EntityNotFoundException("Tutor", id);
        }
        tutorRepository.deleteById(id);
    }

    private TutorResponseDTO toResponseDTO(Tutor tutor) {
        return TutorResponseDTO.builder()
                .id(tutor.getId())
                .name(tutor.getName())
                .email(tutor.getEmail())
                .phone(tutor.getPhone())
                .createdAt(tutor.getCreatedAt())
                .build();
    }

}