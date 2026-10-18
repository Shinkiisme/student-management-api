package com.phuc.studentmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.phuc.studentmanagement.security.RestAccessDeniedHandler;
import com.phuc.studentmanagement.security.RestAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/login")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/students")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/students/{id}")
                .authenticated()
                .requestMatchers(HttpMethod.POST, "/students")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/students/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/students/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/courses")
                .authenticated()
                .requestMatchers(HttpMethod.GET, "/courses/{id}")
                .authenticated()
                .requestMatchers(HttpMethod.POST, "/courses")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/courses/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/courses/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/enrollments")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/enrollments/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/enrollments")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/enrollments/{id}")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/students/{studentId}/enrollments")
                .authenticated()
                .anyRequest()
                .denyAll());

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.csrf(csrf -> csrf.disable());

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler));

        http.oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return jwtConverter;
    }
}