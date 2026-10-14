package com.phuc.studentmanagement.service;

import java.time.Instant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.phuc.studentmanagement.dto.LoginRequestDTO;
import com.phuc.studentmanagement.dto.LoginResponseDTO;
import com.phuc.studentmanagement.model.User;
import com.phuc.studentmanagement.repository.UserRepository;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtEncoder jwtEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
    }

    public Authentication authenticate(LoginRequestDTO request) {
        return authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                .unauthenticated(
                        request.username(),
                        request.password()));
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticate(request);
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        String token = generateToken(user);

        return new LoginResponseDTO(
                token,
                "Bearer",
                900);
    }

    private JwtClaimsSet buildClaims(User user) {
        Instant now = Instant.now();

        return JwtClaimsSet.builder()
                .issuer("student-management-api")
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("role", user.getRole().name())
                .build();
    }

    private String generateToken(User user) {
        JwtClaimsSet claims = buildClaims(user);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(header, claims);

        Jwt jwt = jwtEncoder.encode(parameters);

        return jwt.getTokenValue();
    }
}