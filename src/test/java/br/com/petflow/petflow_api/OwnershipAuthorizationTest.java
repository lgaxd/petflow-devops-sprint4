package br.com.petflow.petflow_api;

import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.dto.PetRequestDTO;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.service.RewardPointService;
import br.com.petflow.petflow_api.service.PetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipAuthorizationTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private RewardPointService rewardPointService;

    @InjectMocks
    private PetService petService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tutorCannotAccessPetFromAnotherTutor() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(1L, "TUTOR"));

        Pet petFromOtherTutor = Pet.builder()
                .id(99L)
                .name("Rex")
                .breed("Labrador")
                .birthDate(LocalDate.of(2020, 1, 1))
                .weight(new BigDecimal("12.50"))
                .speciesId(1L)
                .tutor(Tutor.builder().id(2L).name("Tutor B").build())
                .build();

        when(petRepository.findById(99L)).thenReturn(Optional.of(petFromOtherTutor));

        assertThrows(AccessDeniedException.class, () -> petService.findById(99L));
    }

    @Test
    void tutorCannotCreatePetForAnotherTutor() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(1L, "TUTOR"));
        PetRequestDTO request = new PetRequestDTO(
                "Milo", "Poodle", LocalDate.of(2021, 5, 15), new BigDecimal("4.80"), 2L, 1L);

        assertThrows(AccessDeniedException.class, () -> petService.create(request));
    }

    @Test
    void tutorCanAccessOwnPet() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(1L, "TUTOR"));

        Pet ownPet = Pet.builder()
                .id(10L)
                .name("Milo")
                .breed("Poodle")
                .birthDate(LocalDate.of(2021, 5, 15))
                .weight(new BigDecimal("4.80"))
                .speciesId(2L)
                .tutor(Tutor.builder().id(1L).name("Tutor A").build())
                .build();

        when(petRepository.findById(10L)).thenReturn(Optional.of(ownPet));

        PetResponseDTO response = petService.findById(10L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.tutorId());
    }

    @Test
    void adminCanAccessAnyPet() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(99L, "ADMIN"));

        Pet petFromOtherTutor = Pet.builder()
                .id(80L)
                .name("Luna")
                .breed("Vira-lata")
                .birthDate(LocalDate.of(2019, 2, 10))
                .weight(new BigDecimal("8.40"))
                .speciesId(3L)
                .tutor(Tutor.builder().id(7L).name("Tutor C").build())
                .build();

        when(petRepository.findById(80L)).thenReturn(Optional.of(petFromOtherTutor));

        PetResponseDTO response = petService.findById(80L);

        assertNotNull(response);
        assertEquals(80L, response.id());
        assertEquals(7L, response.tutorId());
    }

    @Test
    void tutorCannotListPetsFromAnotherTutor() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(1L, "TUTOR"));

        assertThrows(AccessDeniedException.class,
                () -> petService.findAll(null, 2L, Pageable.unpaged()));
    }

    @Test
    void tutorNameFilterIsAppliedByTutorScopedRepositoryQuery() {
        SecurityContextHolder.getContext().setAuthentication(authenticationFor(1L, "TUTOR"));
        Pageable pageable = PageRequest.of(0, 10);
        when(petRepository.findByTutorIdAndNameProjected(1L, "Milo", pageable))
                .thenReturn(Page.empty(pageable));

        petService.findAll("Milo", null, pageable);

        verify(petRepository).findByTutorIdAndNameProjected(1L, "Milo", pageable);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(Long tutorId, String role) {
        Tutor tutor = Tutor.builder()
                .id(tutorId)
                .name("Tutor " + tutorId)
                .email("tutor" + tutorId + "@petflow.com")
                .role("ADMIN".equalsIgnoreCase(role) ? UserRole.ADMIN : UserRole.TUTOR)
                .build();

        List<SimpleGrantedAuthority> authorities = "ADMIN".equalsIgnoreCase(role)
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_TUTOR"));

        return new UsernamePasswordAuthenticationToken(tutor, null, authorities);
    }
}
