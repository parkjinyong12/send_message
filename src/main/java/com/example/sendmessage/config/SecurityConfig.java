package com.example.sendmessage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Value("${security.api-key:}")
    private String apiKey;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        boolean jwtConfigured = (issuerUri != null && !issuerUri.isBlank()) || (jwkSetUri != null && !jwkSetUri.isBlank());
        if (jwtConfigured) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            new AntPathRequestMatcher("/health"),
                            new AntPathRequestMatcher("/actuator/**"),
                            new AntPathRequestMatcher("/v3/api-docs/**"),
                            new AntPathRequestMatcher("/swagger-ui/**"),
                            new AntPathRequestMatcher("/swagger-ui.html")
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/messages/send").hasAuthority("SCOPE_msg:send")
                    .anyRequest().authenticated()
            );
        } else if (apiKey != null && !apiKey.isBlank()) {
            http.addFilterBefore(new ApiKeyFilter(apiKey), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            new AntPathRequestMatcher("/health"),
                            new AntPathRequestMatcher("/actuator/**"),
                            new AntPathRequestMatcher("/v3/api-docs/**"),
                            new AntPathRequestMatcher("/swagger-ui/**"),
                            new AntPathRequestMatcher("/swagger-ui.html")
                    ).permitAll()
                    .anyRequest().authenticated()
            );
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    static class ApiKeyFilter extends OncePerRequestFilter {
        private final String expected;
        ApiKeyFilter(String expected) { this.expected = expected; }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            String provided = request.getHeader("X-API-Key");
            if (provided == null || !provided.equals(expected)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            filterChain.doFilter(request, response);
        }
    }
} 