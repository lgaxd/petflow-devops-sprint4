package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.PetRequestDTO;
import br.com.petflow.petflow_api.dto.PetResponseDTO;
import br.com.petflow.petflow_api.entity.Pet;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.PetRepository;
import br.com.petflow.petflow_api.repository.RedeemRepository;
import br.com.petflow.petflow_api.repository.TutorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetAndRedeemServiceTest {

    private final Pageable pageable = PageRequest.of(0, 10);
    private final Tutor owner = Tutor.builder().id(1L).name("Dono").role(UserRole.TUTOR).build();
    private final Tutor admin = Tutor.builder().id(99L).name("Admin").role(UserRole.ADMIN).build();

    @Mock
    private PetRepository petRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private RewardPointService rewardPointService;
    @Mock
    private RedeemRepository redeemRepository;

    @InjectMocks
    private PetService petService;

    @InjectMocks
    private RedeemService redeemService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Tutor user) {
        var authority = user.getRole() == UserRole.ADMIN ? "ROLE_ADMIN" : "ROLE_TUTOR";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private PetRequestDTO request(Long tutorId) {
        return PetRequestDTO.builder().name("Rex").breed("SRD").birthDate(LocalDate.of(2020, 1, 1))
                .weight(new BigDecimal("10.5")).tutorId(tutorId).speciesId(1L).build();
    }

    @Test
    void createGrantsRegistrationPointsToTheOwner() {
        loginAs(owner);
        when(tutorRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(petRepository.save(any(Pet.class))).thenAnswer(call -> {
            Pet saved = call.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        PetResponseDTO response = petService.create(request(1L));

        assertEquals(10L, response.id());
        assertEquals("Dono", response.tutorName());
        verify(rewardPointService).grantPoints(1L, "CADASTRO_PET", "PET", 10L);
    }

    @Test
    void tutorCannotCreateAPetForSomeoneElse() {
        loginAs(owner);

        assertThrows(AccessDeniedException.class, () -> petService.create(request(2L)));
        verify(petRepository, never()).save(any());
    }

    @Test
    void adminCanCreateForAnyExistingTutorButNotForUnknownOnes() {
        loginAs(admin);
        when(tutorRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> petService.create(request(2L)));
    }

    @Test
    void tutorListingIsAlwaysScopedToTheirOwnPets() {
        loginAs(owner);
        when(petRepository.findByTutorIdProjected(1L, pageable)).thenReturn(Page.empty(pageable));
        when(petRepository.findByTutorIdAndNameProjected(1L, "re", pageable)).thenReturn(Page.empty(pageable));

        petService.findAll(null, null, pageable);
        petService.findAll("re", null, pageable);

        verify(petRepository).findByTutorIdProjected(1L, pageable);
        verify(petRepository).findByTutorIdAndNameProjected(1L, "re", pageable);
        assertThrows(AccessDeniedException.class, () -> petService.findAll(null, 2L, pageable));
    }

    @Test
    void adminListingCanSeeEverything() {
        loginAs(admin);
        when(petRepository.findAllProjected(pageable)).thenReturn(Page.empty(pageable));
        when(petRepository.findByNameProjected("re", pageable)).thenReturn(Page.empty(pageable));

        petService.findAll(null, null, pageable);
        petService.findAll("re", null, pageable);

        verify(petRepository).findAllProjected(pageable);
        verify(petRepository).findByNameProjected("re", pageable);
    }

    @Test
    void updateByOwnerChangesDataButOnlyAdminCanTransferTheTutor() {
        Pet pet = Pet.builder().id(10L).name("Old").tutor(owner).build();
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenAnswer(call -> call.getArgument(0));

        loginAs(owner);
        assertEquals("Rex", petService.update(10L, request(1L)).name());
        assertThrows(AccessDeniedException.class, () -> petService.update(10L, request(2L)));

        loginAs(admin);
        Tutor newOwner = Tutor.builder().id(2L).name("Novo").role(UserRole.TUTOR).build();
        when(tutorRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        assertEquals(2L, petService.update(10L, request(2L)).tutorId());
    }

    @Test
    void deleteChecksOwnershipAndExistence() {
        Pet pet = Pet.builder().id(10L).tutor(owner).build();
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));
        when(petRepository.findById(404L)).thenReturn(Optional.empty());

        loginAs(Tutor.builder().id(5L).role(UserRole.TUTOR).build());
        assertThrows(AccessDeniedException.class, () -> petService.delete(10L));
        assertThrows(EntityNotFoundException.class, () -> petService.delete(404L));

        loginAs(owner);
        petService.delete(10L);
        verify(petRepository).delete(pet);
    }

    @Test
    void redeemListingIsScopedToTheLoggedTutorAndChecksTheTutorExists() {
        loginAs(owner);
        when(tutorRepository.existsById(1L)).thenReturn(true);
        when(redeemRepository.findByTutorId(1L, pageable)).thenReturn(Page.empty(pageable));

        redeemService.findAll(null, pageable);

        verify(redeemRepository).findByTutorId(1L, pageable);
        assertThrows(AccessDeniedException.class, () -> redeemService.findAll(2L, pageable));
    }

    @Test
    void adminRedeemListingWithoutFilterReturnsAll() {
        loginAs(admin);
        when(redeemRepository.findAllProjected(pageable)).thenReturn(Page.empty(pageable));

        redeemService.findAll(null, pageable);

        verify(redeemRepository).findAllProjected(pageable);
        when(tutorRepository.existsById(404L)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> redeemService.findAll(404L, pageable));
    }

    @Test
    void redeemLookupOfMissingRecordFails() {
        loginAs(owner);
        when(redeemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> redeemService.findById(404L));
    }
}
