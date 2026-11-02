package br.com.petflow.petflow_api.security;

import br.com.petflow.petflow_api.enums.EnumParser;
import br.com.petflow.petflow_api.enums.UserRole;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            DecodedJWT decoded = jwtService.validateToken(authHeader.substring(BEARER_PREFIX.length()));
            if (decoded != null) {
                authenticate(request, decoded);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, DecodedJWT decoded) {
        UserRole role;
        try {
            role = EnumParser.parse(UserRole.class, decoded.getClaim("role").asString());
        } catch (IllegalArgumentException invalidRole) {
            return; // token sem papel válido não autentica
        }

        AuthenticatedTutor tutor = new AuthenticatedTutor(
                decoded.getClaim("id").asLong(),
                decoded.getSubject(),
                decoded.getClaim("name").asString(),
                null,
                role);

        var authentication = new UsernamePasswordAuthenticationToken(tutor, null, tutor.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
