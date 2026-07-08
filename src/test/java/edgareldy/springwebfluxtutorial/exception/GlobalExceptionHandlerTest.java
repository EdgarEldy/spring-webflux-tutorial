package edgareldy.springwebfluxtutorial.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in GlobalExceptionHandler's exception to status mapping, in particular that a
 * ResponseStatusException thrown natively by WebFlux itself (e.g. for an unsupported HTTP
 * method) keeps its own status instead of being swallowed into a generic 500 by the catch-all
 * branch.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ObjectMapper());

    @Test
    void mapsResourceNotFoundExceptionTo404() {
        assertStatus(new ResourceNotFoundException("not found"), HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsBusinessRuleExceptionTo422() {
        assertStatus(new BusinessRuleException("rule violated"), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void mapsAuthenticationExceptionTo401() {
        assertStatus(new BadCredentialsException("bad credentials"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mapsServerWebInputExceptionTo400() {
        assertStatus(new ServerWebInputException("malformed body"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsNativeResponseStatusExceptionToItsOwnStatusInsteadOfGeneric500() {
        assertStatus(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void mapsUnrecognizedExceptionTo500() {
        assertStatus(new RuntimeException("boom"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void assertStatus(Throwable ex, HttpStatus expected) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test").build());

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expected);
    }
}
