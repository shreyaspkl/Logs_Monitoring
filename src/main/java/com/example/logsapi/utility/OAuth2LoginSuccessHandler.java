package com.example.logsapi.utility;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.example.logsapi.model.User;
import com.example.logsapi.repository.UserRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.frontend.redirect-url}")
    private String frontendRedirectUrl;

    public OAuth2LoginSuccessHandler(JwtService jwtService,
                                     UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected auth type");
            return;
        }

        OAuth2User oauth2User = oauthToken.getPrincipal();
        Map<String, Object> attrs = oauth2User.getAttributes();

        // These attribute names depend on provider; for Google:
        String email = (String) attrs.get("email");
        String name = (String) attrs.getOrDefault("name", email);

        if (email == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by provider");
            return;
        }

        // Find or create local user
        Optional<User> existingOpt = userRepository.findByUsername(email);
        User user;
        if (existingOpt.isPresent()) {
            user = existingOpt.get();
        } else {
            user = new User();
            user.setUsername(email);  // or separate username field
            user.setEmail(email);
            // No local password for OAuth users; leave passwordHash null or special value
            userRepository.save(user);
        }

        // Issue your own JWT
        String token = jwtService.generateToken(user);

        // Redirect back to frontend with token (e.g. as query param)
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String redirectUrl = frontendRedirectUrl + "?jwt=" + encodedToken;

        response.sendRedirect(redirectUrl);
    }
}