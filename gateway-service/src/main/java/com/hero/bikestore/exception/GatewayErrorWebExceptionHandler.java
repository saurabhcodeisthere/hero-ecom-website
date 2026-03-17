package com.hero.bikestore.exception;

import tools.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catches exceptions that escape Spring Security filters and the gateway routing layer.
 *
 * Why @Order(-2)?
 * - Spring Boot registers its own DefaultErrorWebExceptionHandler at @Order(-1).
 * - We need a lower order value (higher precedence) so our handler runs first.
 * - @Order(-2) beats -1 and wins every time.
 *
 * What does this catch?
 * - 404 Not Found  — no gateway route matched the incoming request path
 * - 503 Service Unavailable — Eureka found the service but it's down / connection refused
 * - Any other ResponseStatusException the gateway throws during routing
 *
 * What does NOT come here?
 * - 401 / 403 from Spring Security — those are handled by
 *   GatewayAuthenticationEntryPoint and GatewayAccessDeniedHandler respectively.
 */
@Order(-2)
@Component
public class GatewayErrorWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status;
        String errorCode;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status    = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

            errorCode = deriveErrorCode(status);
            message   = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();

        } else {
            // Unexpected / unclassified error — treat as 500
            status    = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_SERVER_ERROR";
            message   = "An unexpected error occurred at the gateway";
        }

        return writeErrorResponse(exchange.getResponse(), status, errorCode, message);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String deriveErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND            -> "ROUTE_NOT_FOUND";
            case SERVICE_UNAVAILABLE  -> "SERVICE_UNAVAILABLE";
            case BAD_GATEWAY          -> "BAD_GATEWAY";
            case GATEWAY_TIMEOUT      -> "GATEWAY_TIMEOUT";
            case TOO_MANY_REQUESTS    -> "RATE_LIMIT_EXCEEDED";
            default                   -> status.name();
        };
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

        byte[] bytes   = objectMapper.writeValueAsBytes(body);
        DataBuffer buf = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buf));
    }
}
