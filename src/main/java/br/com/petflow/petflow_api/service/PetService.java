package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.PetRequestDTO;
import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final RewardPointService rewardPointService;

    @Transactional
    public PetResponseDTO create(PetRequestDTO request) {
        if (!SecurityUtils.isAdmin()
                && !SecurityUtils.getCurrentTutorId().equals(request.tutorId())) {
            throw new AccessDeniedException("Você não pode cadastrar pets para outro tutor.");
        }

        Tutor tutor = tutorRepository.findById(request.tutorId())
                .orElseThrow(() -> new EntityNotFoundException("Tutor", request.tutorId()));

        Pet pet = Pet.builder()
                .name(request.name())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .weight(request.weight())
                .speciesId(request.speciesId())
                .tutor(tutor)
                .build();

        pet = petRepository.save(pet);

        rewardPointService.grantPoints(tutor.getId(), "CADASTRO_PET", "PET", pet.getId());

        return toResponseDTO(pet);
    }

    @Transactional(readOnly = true)
    public PetResponseDTO findById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pet", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(pet.getTutor().getId());
        }

        return toResponseDTO(pet);
    }

    @Transactional(readOnly = true)
    public Page<PetResponseDTO> findAll(String name, Long tutorId, Pageable pageable) {
        if (!SecurityUtils.isAdmin()) {
            Long currentTutorId = SecurityUtils.getCurrentTutorId();
            if (tutorId != null && !currentTutorId.equals(tutorId)) {
                throw new AccessDeniedException("Você não tem permissão para listar pets de outro tutor.");
            }
            tutorId = currentTutorId;
        }

        if (name != null && !name.isBlank()) {
            if (!SecurityUtils.isAdmin()) {
                return petRepository.findByTutorIdAndNameProjected(tutorId, name, pageable);
            }
            return petRepository.findByNameProjected(name, pageable);
        } else if (tutorId != null) {
            return petRepository.findByTutorIdProjected(tutorId, pageable);
        }
        return petRepository.findAllProjected(pageable);
    }

    @Transactional
    public PetResponseDTO update(Long id, PetRequestDTO request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pet", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(pet.getTutor().getId());
        }

        if (request.tutorId() != null && !pet.getTutor().getId().equals(request.tutorId())) {
            if (!SecurityUtils.isAdmin()) {
                throw new AccessDeniedException("Você não pode transferir um pet para outro tutor.");
            }
            Tutor tutor = tutorRepository.findById(request.tutorId())
                    .orElseThrow(() -> new EntityNotFoundException("Tutor", request.tutorId()));
            pet.setTutor(tutor);
        }

        pet.setName(request.name());
        pet.setBreed(request.breed());
        pet.setBirthDate(request.birthDate());
        pet.setWeight(request.weight());
        pet.setSpeciesId(request.speciesId());
        pet = petRepository.save(pet);
        return toResponseDTO(pet);
    }

    @Transactional
    public void delete(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pet", id));

        if (!SecurityUtils.isAdmin()) {
            SecurityUtils.checkOwnership(pet.getTutor().getId());
        }

        petRepository.delete(pet);
    }

    private PetResponseDTO toResponseDTO(Pet pet) {
        return PetResponseDTO.builder()
                .id(pet.getId())
                .name(pet.getName())
                .breed(pet.getBreed())
                .birthDate(pet.getBirthDate())
                .weight(pet.getWeight())
                .speciesId(pet.getSpeciesId())
                .createdAt(pet.getCreatedAt())
                .tutorId(pet.getTutor().getId())
                .tutorName(pet.getTutor().getName())
                .build();
    }
}