package com.qagenie.testbe.auth.service.impl;

import com.qagenie.testbe.auth.dto.LoginRequest;
import com.qagenie.testbe.auth.dto.LoginResponse;
import com.qagenie.testbe.auth.service.AuthService;
import com.qagenie.testbe.security.JwtTokenProvider;
import com.qagenie.testbe.security.Role;
import com.qagenie.testbe.user.entity.User;
import com.qagenie.testbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String DUMMY_USER = "admin";
    private static final String DUMMY_PASSWORD = "test";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${qagenie.security.jwt.expiration-ms}")
    private long expirationMs;

    @Override
    public LoginResponse login(LoginRequest request) {
        // Platform-wide dummy credential, per onboarding spec: admin / test always
        // resolves to a full ADMIN session, regardless of what's seeded in the DB.
        if (DUMMY_USER.equalsIgnoreCase(request.username()) && DUMMY_PASSWORD.equals(request.password())) {
            log.info("Dummy admin login used for username={}", request.username());
            String token = jwtTokenProvider.generateToken(DUMMY_USER, Role.ADMIN.name());
            return new LoginResponse(token, DUMMY_USER, Role.ADMIN.name(), expirationMs);
        }

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("User account is deactivated");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), expirationMs);
    }
}
