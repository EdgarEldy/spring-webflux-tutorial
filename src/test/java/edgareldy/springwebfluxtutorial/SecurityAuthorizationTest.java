package edgareldy.springwebfluxtutorial;

import edgareldy.springwebfluxtutorial.dto.auth.AuthResponse;
import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * End-to-end test proving the full reactive security chain works together with a real JWT,
 * not a mocked principal: register issues an account, login issues a token, and that token is
 * then presented on real requests to prove JwtAuthWebFilter actually authenticates it and
 * SecurityConfig's authorization rules actually apply. Every other security-adjacent test in
 * this project mocks either the service layer or the Authentication itself; this is the one
 * place the whole chain is exercised for real, the same category of gap a sibling project on
 * this account (spring-boot-tutorial) found the hard way after shipping feature/auth without
 * it.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
class SecurityAuthorizationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicGetEndpointIsReachableWithoutAuthentication() {
        webTestClient.get().uri("/api/v1/categories")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void writeEndpointWithoutTokenReturns401() {
        webTestClient.post().uri("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Unauthorized attempt"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedEndpointWithoutTokenReturns401() {
        webTestClient.get().uri("/api/v1/customers")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void registerLoginThenAccessAuthenticatedEndpointWithRealToken() {
        String username = "e2e-user-" + UUID.randomUUID();

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(username, "supersecret1", username + "@example.com"))
                .exchange()
                .expectStatus().isCreated();

        ApiResponse<AuthResponse> loginBody = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, "supersecret1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        String token = loginBody.data().token();

        webTestClient.get().uri("/api/v1/customers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.username").isEqualTo(username);
    }

    @Test
    void freshlyRegisteredUserCannotWriteToAdminOnlyEndpoint() {
        String username = "e2e-user-" + UUID.randomUUID();

        webTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(username, "supersecret1", username + "@example.com"))
                .exchange()
                .expectStatus().isCreated();

        ApiResponse<AuthResponse> loginBody = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, "supersecret1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        String token = loginBody.data().token();

        webTestClient.post().uri("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Should be forbidden"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminUserCanWriteToAdminOnlyEndpoint() {
        String username = "e2e-admin-" + UUID.randomUUID();

        appUserRepository.save(AppUser.builder()
                        .username(username)
                        .password(passwordEncoder.encode("supersecret1"))
                        .email(username + "@example.com")
                        .role(Role.ADMIN)
                        .build())
                .block();

        ApiResponse<AuthResponse> loginBody = webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(username, "supersecret1"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        String token = loginBody.data().token();

        webTestClient.post().uri("/api/v1/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Allowed for admin " + username))
                .exchange()
                .expectStatus().isCreated();
    }
}
