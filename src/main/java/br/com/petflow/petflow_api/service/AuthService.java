package br.com.petflow.petflow_api.service;

import br.com.petflow.petflow_api.dto.LoginRequestDTO;
import br.com.petflow.petflow_api.dto.LoginResponseDTO;
import br.com.petflow.petflow_api.entity.Tutor;
import br.com.petflow.petflow_api.enums.UserRole;
import br.com.petflow.petflow_api.exception.InvalidTokenException;
import br.com.petflow.petflow_api.repository.TutorRepository;
import br.com.petflow.petflow_api.security.AuthenticatedTutor;
import br.com.petflow.petflow_api.security.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final TutorRepository tutorRepository;
    private final JwtService jwtService;

    public LoginResponseDTO login(LoginRequestDTO request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AuthenticatedTutor tutor = (AuthenticatedTutor) authentication.getPrincipal();
        String token = jwtService.generateToken(tutor);

        return toResponse(token, tutor.getId(), tutor.getName(), tutor.getEmail(), tutor.getRole());
    }

    /**
     * Troca um token ainda válido por um novo. Os dados (nome, e-mail, perfil) são relidos do banco,
     * então um usuário removido deixa de renovar e uma mudança de perfil vale no novo token. O início
     * da sessão original é preservado: depois do tempo máximo é preciso fazer login de novo.
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO refresh(String authorizationHeader) {
        String rawToken = stripBearer(authorizationHeader);
        DecodedJWT current = rawToken != null ? jwtService.validateToken(rawToken) : null;
        if (current == null) {
            throw InvalidTokenException.invalid();
        }
        if (jwtService.isSessionExpired(current)) {
            throw InvalidTokenException.sessionExpired();
        }

        Tutor tutor = tutorRepository.findById(current.getClaim("id").asLong())
                .orElseThrow(InvalidTokenException::invalid);
        String token = jwtService.generateRefreshedToken(tutor, current);

        return toResponse(token, tutor.getId(), tutor.getName(), tutor.getEmail(), tutor.getRole());
    }

    private String stripBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private LoginResponseDTO toResponse(String token, Long id, String name, String email, UserRole role) {
        return LoginResponseDTO.builder()
                .token(token)
                .id(id)
                .name(name)
                .email(email)
                .role(role)
                .expiresAt(jwtService.extractExpiration(token))
                .build();
    }
}
