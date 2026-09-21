package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import edgareldy.springwebfluxtutorial.exception.GlobalExceptionHandler;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.security.JwtService;
import edgareldy.springwebfluxtutorial.service.ProductService;
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
 * WebTestClient tests for ProductController, covering the nominal path, category filtering,
 * and the 404/400 cases handled by GlobalExceptionHandler (imported explicitly since
 * @WebFluxTest does not pick up a plain WebExceptionHandler bean by default). Security
 * auto-configuration is excluded so SecurityConfig's real authorization rules do not apply
 * here (see SecurityAuthorizationTest for that). JwtService is mocked because @WebFluxTest
 * still instantiates any WebFilter bean present on the classpath regardless of excluded
 * auto-configuration, and JwtAuthWebFilter needs it to construct.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@WebFluxTest(controllers = ProductController.class,
        excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void _01_ShouldReturnWrappedPage_WhenProductsAreListed() {
        CategoryResponse category = new CategoryResponse(1L, "Electronics");
        ProductResponse product = new ProductResponse(1L, category, "Laptop", 999f);
        PageResponse<ProductResponse> page = PageResponse.of(List.of(product), 0, 20, 1);
        when(productService.findAll(0, 20, null)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content[0].productName").isEqualTo("Laptop")
                .jsonPath("$.data.content[0].category.categoryName").isEqualTo("Electronics");
    }

    @Test
    void _02_ShouldFilterProducts_WhenCategoryIdIsGiven() {
        CategoryResponse category = new CategoryResponse(1L, "Electronics");
        ProductResponse product = new ProductResponse(1L, category, "Laptop", 999f);
        PageResponse<ProductResponse> page = PageResponse.of(List.of(product), 0, 20, 1);
        when(productService.findAll(0, 20, 1L)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/products?categoryId=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.content[0].productName").isEqualTo("Laptop");
    }

    @Test
    void _03_ShouldReturn404_WhenProductIsMissing() {
        when(productService.findById(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Product not found with id 99")));

        webTestClient.get().uri("/api/v1/products/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void _04_ShouldReturn201_WhenProductIsCreated() {
        CategoryResponse category = new CategoryResponse(1L, "Electronics");
        ProductResponse response = new ProductResponse(1L, category, "Laptop", 999f);
        when(productService.create(any(ProductRequest.class))).thenReturn(Mono.just(response));

        webTestClient.post().uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProductRequest(1L, "Laptop", 999f))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.productName").isEqualTo("Laptop");
    }

    @Test
    void _05_ShouldReturn400_WhenValidationFails() {
        webTestClient.post().uri("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProductRequest(null, "", -5f))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void _06_ShouldReturn200_WhenProductIsUpdated() {
        CategoryResponse category = new CategoryResponse(2L, "Premium");
        ProductResponse response = new ProductResponse(1L, category, "Laptop Pro", 1299f);
        when(productService.update(eq(1L), any(ProductRequest.class))).thenReturn(Mono.just(response));

        webTestClient.put().uri("/api/v1/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProductRequest(2L, "Laptop Pro", 1299f))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.productName").isEqualTo("Laptop Pro");
    }

    @Test
    void _07_ShouldReturn404_WhenUpdatedProductIsMissing() {
        when(productService.update(eq(99L), any(ProductRequest.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("Product not found with id 99")));

        webTestClient.put().uri("/api/v1/products/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProductRequest(2L, "Laptop Pro", 1299f))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void _08_ShouldReturn200_WhenProductIsDeleted() {
        when(productService.delete(1L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/v1/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
