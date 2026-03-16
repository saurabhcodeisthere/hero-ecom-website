package com.hero.bikestore.security;

import tools.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GatewayAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {

        String errorCode = "UNAUTHORIZED";
        String message   = "Authentication required. Please provide a valid token";

        // Spring Security wraps ALL JWT issues inside InvalidBearerTokenException
        if (authException instanceof InvalidBearerTokenException invalidBearerEx) {
            Throwable cause = invalidBearerEx.getCause();

            if (cause instanceof JwtValidationException jwtValidationEx) {
                // Semantic failures: expired, wrong issuer, wrong audience etc.
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
                // Structural failures: malformed token, bad signature, wrong format
                errorCode = "TOKEN_INVALID";
                message   = "JWT token is malformed or has an invalid signature";

            } else {
                errorCode = "TOKEN_INVALID";
                message   = invalidBearerEx.getMessage();
            }
        }

        return writeErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response,
                                          HttpStatus status,
                                          String errorCode,
                                          String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("message",   message);
        body.put("errorCode", errorCode);

        // Jackson 3.x (tools.jackson) — writeValueAsBytes throws unchecked JacksonException
        byte[] bytes   = objectMapper.writeValueAsBytes(body);
        DataBuffer buf = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buf));
    }
}
