package com.avaricia.sb_service.authentication.config;

import com.avaricia.sb_service.authentication.model.AuthProvider;
import com.avaricia.sb_service.authentication.model.User;
import com.avaricia.sb_service.authentication.repository.UserRepository;
import com.avaricia.sb_service.authentication.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        
        String email;
        String name;
        String providerId;
        AuthProvider provider;
        
        if ("google".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            providerId = oAuth2User.getAttribute("sub");
            provider = AuthProvider.GOOGLE;
        } else if ("microsoft".equals(registrationId)) {
            email = oAuth2User.getAttribute("email");
            if (email == null) {
                email = oAuth2User.getAttribute("preferred_username");
            }
            name = oAuth2User.getAttribute("name");
            providerId = oAuth2User.getAttribute("sub");
            if (providerId == null) {
                providerId = oAuth2User.getAttribute("oid");
            }
            provider = AuthProvider.MICROSOFT;
        } else {
            throw new RuntimeException("Unsupported OAuth2 provider: " + registrationId);
        }
        
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Could not get email from provider: " + registrationId);
        }
        
        final String finalEmail = email;
        final String finalName = name;
        final String finalProviderId = providerId;
        final AuthProvider finalProvider = provider;
        
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(finalEmail)
                            .name(finalName != null ? finalName : finalEmail.split("@")[0])
                            .provider(finalProvider)
                            .providerId(finalProviderId)
                            .build();
                    return userRepository.save(newUser);
                });
        
        String token = jwtService.generateToken(user);
        
        String redirectUrl = frontendUrl + "/oauth2/callback?token=" + token 
                + "&username=" + URLEncoder.encode(user.getName() != null ? user.getName() : "", StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                + "&provider=" + user.getProvider().name();
        
        response.sendRedirect(redirectUrl);
    }
}
