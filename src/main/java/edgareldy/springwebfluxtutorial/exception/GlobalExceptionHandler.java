package edgareldy.springwebfluxtutorial.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Centralized, non-blocking exception handler that translates every exception raised while
 * handling a request, whether it comes from an annotated controller or a functional
 * RouterFunction, into a consistent ApiResponse<ErrorResponse>. Implemented as a
 * WebExceptionHandler rather than a @RestControllerAdvice so both API styles used in this
 * project are covered by the same handler.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Component
@Order(-2)
public class GlobalExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);
        List<ErrorResponse.FieldError> fieldErrors = resolveFieldErrors(ex);

        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled exception on {} {}", exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath(), ex);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                fieldErrors
        );
        ApiResponse<ErrorResponse> body = new ApiResponse<>(false, message, errorResponse, errorResponse.timestamp());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = new byte[0];
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof BusinessRuleException) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (ex instanceof AuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ex instanceof WebExchangeBindException || ex instanceof ServerWebInputException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof WebExchangeBindException) {
            return "Validation failed";
        }
        if (ex instanceof ServerWebInputException) {
            return "Malformed request body";
        }
        if (ex instanceof AuthenticationException) {
            return "Invalid username or password";
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            String reason = responseStatusException.getReason();
            return reason != null ? reason : status.getReasonPhrase();
        }
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return "Unexpected error";
        }
        return ex.getMessage();
    }

    private List<ErrorResponse.FieldError> resolveFieldErrors(Throwable ex) {
        if (ex instanceof WebExchangeBindException bindException) {
            return bindException.getFieldErrors().stream()
                    .map(fieldError -> new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                    .toList();
        }
        return null;
    }
}
