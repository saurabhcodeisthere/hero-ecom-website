package com.hero.bikestore.security;

import com.hero.bikestore.common.exception.model.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        String errorCode = "UNAUTHORIZED";
        String message   = "Authentication required. Please provide a valid token";

        // Spring Security throws InvalidBearerTokenException for ALL JWT issues
        if (authException instanceof InvalidBearerTokenException invalidBearerEx) {
            Throwable cause = invalidBearerEx.getCause();

            if (cause instanceof JwtValidationException jwtValidationEx) {
                // JwtValidationException is thrown for semantic failures:
                // expired token, wrong issuer, wrong audience etc.
                boolean isExpired = jwtValidationEx.getErrors().stream()
                        .anyMatch(error -> error.getDescription()
                                .toLowerCase().contains("expired"));

                if (isExpired) {
                    errorCode = "TOKEN_EXPIRED";
                    message   = "JWT token has expired. Please login again";
                } else {
                    errorCode = "TOKEN_INVALID";
                    message   = "JWT token validation failed: " + jwtValidationEx.getMessage();
                }

            } else if (cause instanceof BadJwtException) {
                // BadJwtException is thrown for structural failures:
                // malformed token, bad signature, wrong format etc.
                errorCode = "TOKEN_INVALID";
                message   = "JWT token is malformed or has an invalid signature";

            } else {
                // Any other bearer token issue
                errorCode = "TOKEN_INVALID";
                message   = invalidBearerEx.getMessage();
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(message, errorCode);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
