package br.com.petflow.petflow_api.security;

import br.com.petflow.petflow_api.entity.Tutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentTutorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedTutor tutor) {
            if (tutor.getId() == null) {
                throw new AccessDeniedException("Tutor autenticado sem identificação disponível.");
            }
            return tutor.getId();
        }

        if (principal instanceof Tutor tutor) {
            if (tutor.getId() == null) {
                throw new AccessDeniedException("Tutor autenticado sem identificação disponível.");
            }
            return tutor.getId();
        }

        throw new AccessDeniedException("Usuário autenticado não é um tutor válido.");
    }

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        for (GrantedAuthority granted : authentication.getAuthorities()) {
            String authority = granted.getAuthority();
            if ("ROLE_ADMIN".equals(authority) || "ADMIN".equals(authority)) {
                return true;
            }
        }
        return false;
    }

    public static void checkOwnership(Long resourceOwnerTutorId) {
        if (resourceOwnerTutorId == null) {
            return;
        }

        if (isAdmin()) {
            return;
        }

        Long currentTutorId = getCurrentTutorId();
        if (!Objects.equals(currentTutorId, resourceOwnerTutorId)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este recurso.");
        }
    }
}
