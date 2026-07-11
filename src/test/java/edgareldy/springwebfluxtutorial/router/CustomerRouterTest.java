package edgareldy.springwebfluxtutorial.router;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerRequest;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerResponse;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.GlobalExceptionHandler;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.service.CustomerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * WebTestClient tests for the Customer functional endpoints (CustomerRouter + CustomerHandler),
 * covering the nominal path plus the 400 (manual validation), 404 and 422 cases. Security
 * auto-configuration is excluded since no SecurityConfig exists yet on this branch.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@WebFluxTest(controllers = CustomerRouter.class,
        excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@Import({CustomerHandler.class, GlobalExceptionHandler.class})
class CustomerRouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void findAllReturnsWrappedPage() {
        CustomerResponse customer = new CustomerResponse(1L, "Ada", "Lovelace", "555-0100", "ada@example.com", "1 Main St");
        PageResponse<CustomerResponse> page = PageResponse.of(List.of(customer), 0, 20, 1);
        when(customerService.findAll(0, 20, null)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/customers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content[0].lastName").isEqualTo("Lovelace");
    }

    @Test
    void findAllSearchesByNameWhenSearchProvided() {
        CustomerResponse customer = new CustomerResponse(1L, "Ada", "Lovelace", "555-0100", "ada@example.com", "1 Main St");
        PageResponse<CustomerResponse> page = PageResponse.of(List.of(customer), 0, 20, 1);
        when(customerService.findAll(0, 20, "lovelace")).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/customers?search=lovelace")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content[0].lastName").isEqualTo("Lovelace");
    }

    @Test
    void findByIdReturns404WhenMissing() {
        when(customerService.findById(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Customer not found with id 99")));

        webTestClient.get().uri("/api/v1/customers/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void createReturns201OnSuccess() {
        CustomerResponse response = new CustomerResponse(1L, "Ada", "Lovelace", "555-0100", "ada@example.com", "1 Main St");
        when(customerService.create(any(CustomerRequest.class))).thenReturn(Mono.just(response));

        webTestClient.post().uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CustomerRequest("Ada", "Lovelace", "555-0100", "ada@example.com", "1 Main St"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.email").isEqualTo("ada@example.com");
    }

    @Test
    void createReturns400OnManualValidationFailure() {
        webTestClient.post().uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CustomerRequest("", "", "", "not-an-email", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void findByIdReturns400WhenIdIsNotNumeric() {
        webTestClient.get().uri("/api/v1/customers/not-a-number")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void findAllReturns400WhenPageIsNotNumeric() {
        webTestClient.get().uri("/api/v1/customers?page=not-a-number")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createReturns422WhenEmailAlreadyTaken() {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(Mono.error(
                new BusinessRuleException("Email ada@example.com is already in use")));

        webTestClient.post().uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CustomerRequest("Ada", "Lovelace", "555-0100", "ada@example.com", "1 Main St"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void updateReturns200OnSuccess() {
        CustomerResponse response = new CustomerResponse(1L, "Ada", "Lovelace", "555-0199", "ada@example.com", "2 Main St");
        when(customerService.update(eq(1L), any(CustomerRequest.class))).thenReturn(Mono.just(response));

        webTestClient.put().uri("/api/v1/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CustomerRequest("Ada", "Lovelace", "555-0199", "ada@example.com", "2 Main St"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.address").isEqualTo("2 Main St");
    }

    @Test
    void deleteReturns200OnSuccess() {
        when(customerService.delete(1L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/v1/customers/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
