package br.com.petflow.petflow_api.config;

import br.com.petflow.petflow_api.security.JwtAccessDeniedHandler;
import br.com.petflow.petflow_api.security.JwtAuthenticationEntryPoint;
import br.com.petflow.petflow_api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${CORS_ALLOWED_ORIGINS}")
    private String allowedOrigins;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/swagger", "/swagger/**", "/swagger-ui/**", "/swagger-ui.html",
            "/api-docs", "/api-docs/**",
            "/actuator/health", "/actuator/health/**", "/actuator/info"
    };

    /**
     * O filtro JWT pertence à cadeia do Spring Security; sem isto o Spring Boot
     * também o registraria como filtro de servlet, fora da ordem de segurança.
     */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Precisa vir antes de /auth/**: a renovação exige um token válido
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").authenticated()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Cadastro público de tutor (novo usuário se autocadastra no app)
                        .requestMatchers(HttpMethod.POST, "/tutors").permitAll()

                        // Gestão de tutores é responsabilidade do ADMIN (consulta, edição, remoção)
                        .requestMatchers(HttpMethod.GET, "/tutors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/tutors/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/tutors/**").hasRole("ADMIN")

                        // Cadastros administrativos (clínicas, planos, cupons) só o ADMIN cria/edita/remove
                        .requestMatchers(HttpMethod.POST, "/clinics/**", "/plans/**", "/coupons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/clinics/**", "/plans/**", "/coupons/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/clinics/**", "/plans/**", "/coupons/**").hasRole("ADMIN")

                        // Consultas (GET) e os fluxos do dia a dia do tutor exigem apenas login
                        .requestMatchers(HttpMethod.GET, "/clinics/**", "/plans/**", "/coupons/**").authenticated()
                        .requestMatchers("/pets/**", "/health-events/**", "/subscriptions/**", "/redeems/**").authenticated()
                        .requestMatchers("/gamification/**").authenticated()

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
            .map(origin -> origin.trim())
            .filter(origin -> !origin.isEmpty())
            .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
