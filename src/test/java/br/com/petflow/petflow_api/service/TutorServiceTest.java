package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.TutorRequestDTO;
import br.com.petflow.petflow_api.dto.TutorUpdateRequestDTO;
import br.com.petflow.petflow_api.dto.TutorResponseDTO;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.DuplicateResourceException;
import br.com.petflow.petflow_api.exception.EntityNotFoundException;
import br.com.petflow.petflow_api.repository.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorServiceTest {

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TutorService tutorService;

    private TutorRequestDTO request(String email, String password) {
        return TutorRequestDTO.builder().name("Maria").email(email).phone("1199").password(password).build();
    }

    private TutorUpdateRequestDTO updateRequest(String email, String password) {
        return TutorUpdateRequestDTO.builder().name("Maria").email(email).phone("1199").password(password).build();
    }

    @Test
    void createHashesPasswordAndAlwaysAssignsTutorRole() {
        when(tutorRepository.existsByEmail("maria@petflow.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("HASH");
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(call -> call.getArgument(0));

        TutorResponseDTO response = tutorService.create(request("maria@petflow.com", "senha1234"));

        ArgumentCaptor<Tutor> saved = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).save(saved.capture());
        assertEquals("HASH", saved.getValue().getPasswordHash());
        assertEquals(UserRole.TUTOR, saved.getValue().getRole());
        assertEquals("maria@petflow.com", response.email());
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(tutorRepository.existsByEmail("maria@petflow.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> tutorService.create(request("maria@petflow.com", "senha1234")));
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void updateKeepsPasswordWhenNoneIsProvided() {
        Tutor existing = Tutor.builder().id(5L).name("Old").email("maria@petflow.com").passwordHash("OLD").role(UserRole.TUTOR).build();
        when(tutorRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(tutorRepository.save(any(Tutor.class))).thenAnswer(call -> call.getArgument(0));

        TutorResponseDTO response = tutorService.update(5L, updateRequest("maria@petflow.com", " "));

        assertEquals("OLD", existing.getPasswordHash());
        assertEquals("Maria", response.name());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateRejectsEmailOwnedByAnotherTutor() {
        Tutor existing = Tutor.builder().id(5L).email("a@petflow.com").role(UserRole.TUTOR).build();
        when(tutorRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(tutorRepository.existsByEmail("b@petflow.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> tutorService.update(5L, updateRequest("b@petflow.com", "senha1234")));
    }

    @Test
    void findByIdAndDeleteFailForUnknownTutor() {
        when(tutorRepository.findById(9L)).thenReturn(Optional.empty());
        when(tutorRepository.existsById(9L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> tutorService.findById(9L));
        assertThrows(EntityNotFoundException.class, () -> tutorService.delete(9L));
    }
}
