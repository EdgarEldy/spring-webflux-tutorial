package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.auth.AuthResponse;
import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.exception.GlobalExceptionHandler;
import edgareldy.springwebfluxtutorial.security.JwtService;
import edgareldy.springwebfluxtutorial.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * WebTestClient tests for AuthController, covering register/login plus the 400/401 cases
 * GlobalExceptionHandler handles. Security auto-configuration is excluded so SecurityConfig's
 * real authorization rules do not apply here. GET /me is deliberately not covered by this
 * slice test: excluding security auto-configuration also removes the
 * @AuthenticationPrincipal argument resolver bean, so it always resolves to null regardless of
 * any mocked Authentication, which would make a slice-level assertion here meaningless.
 * SecurityAuthorizationTest exercises /me for real instead, against the actual filter chain
 * with a genuine JWT, which is the only way to prove it correctly.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@WebFluxTest(controllers = AuthController.class,
        excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void registerReturns201OnSuccess() {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.just(new AuthResponse("token123", "ada", "USER")));

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("ada", "supersecret", "ada@example.com"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.username").isEqualTo("ada");
    }

    @Test
    void registerReturns400OnValidationFailure() {
        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("", "short", "not-an-email"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void loginReturns200OnSuccess() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(new AuthResponse("token456", "ada", "ADMIN")));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("ada", "supersecret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.role").isEqualTo("ADMIN");
    }

    @Test
    void loginReturns401OnBadCredentials() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new BadCredentialsException("Invalid username or password")));

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("ada", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
