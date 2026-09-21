package edgareldy.springwebfluxtutorial.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
    void _01_ShouldMapTo404_WhenResourceNotFoundExceptionIsHandled() {
        assertStatus(new ResourceNotFoundException("not found"), HttpStatus.NOT_FOUND);
    }

    @Test
    void _02_ShouldMapTo422_WhenBusinessRuleExceptionIsHandled() {
        assertStatus(new BusinessRuleException("rule violated"), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void _03_ShouldMapTo401_WhenAuthenticationExceptionIsHandled() {
        assertStatus(new BadCredentialsException("bad credentials"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mapsGenericAuthenticationExceptionTo401() {
        assertStatus(new AuthenticationCredentialsNotFoundException("no credentials"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mapsAccessDeniedExceptionTo403() {
        assertStatus(new AccessDeniedException("denied"), HttpStatus.FORBIDDEN);
    }

    @Test
    void _04_ShouldMapTo400_WhenServerWebInputExceptionIsHandled() {
        assertStatus(new ServerWebInputException("malformed body"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void _05_ShouldMapToItsOwnStatus_WhenNativeResponseStatusExceptionIsHandled() {
        assertStatus(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void _06_ShouldMapTo500_WhenUnrecognizedExceptionIsHandled() {
        assertStatus(new RuntimeException("boom"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void assertStatus(Throwable ex, HttpStatus expected) {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/test").build());

        StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expected);
    }
}
