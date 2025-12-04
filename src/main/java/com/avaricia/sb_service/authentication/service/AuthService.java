package com.avaricia.sb_service.authentication.service;

import com.avaricia.sb_service.authentication.dto.AuthResponse;
import com.avaricia.sb_service.authentication.dto.LoginRequest;
import com.avaricia.sb_service.authentication.dto.RegisterRequest;
import com.avaricia.sb_service.authentication.model.AuthProvider;
import com.avaricia.sb_service.authentication.model.User;
import com.avaricia.sb_service.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        var user = User.builder()
                .name(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .build();
        
        userRepository.save(user);
        
        var jwtToken = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getName())
                .email(user.getEmail())
                .provider(AuthProvider.LOCAL.name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("This user registered with " + user.getProvider().name() + ". Please use that method to sign in.");
        }
        
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new RuntimeException("This user does not have a password configured");
        }
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        
        var jwtToken = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .username(user.getName())
                .email(user.getEmail())
                .provider(AuthProvider.LOCAL.name())
                .build();
    }
}
