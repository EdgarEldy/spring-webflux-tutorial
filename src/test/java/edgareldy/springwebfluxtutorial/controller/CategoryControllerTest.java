package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.GlobalExceptionHandler;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.service.CategoryService;
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
 * WebTestClient tests for CategoryController, covering the nominal path plus the 404/400/422
 * cases handled by GlobalExceptionHandler (imported explicitly since @WebFluxTest does not
 * pick up a plain WebExceptionHandler bean by default). Security auto-configuration is
 * excluded since no SecurityConfig exists yet on this branch (feature/auth is where
 * authorization rules on these endpoints get introduced); without this exclusion Spring
 * Boot's default reactive security would secure every route and reject every request here.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@WebFluxTest(controllers = CategoryController.class,
        excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void findAllReturnsWrappedPage() {
        CategoryResponse category = new CategoryResponse(1L, "Electronics");
        PageResponse<CategoryResponse> page = PageResponse.of(List.of(category), 0, 20, 1);
        when(categoryService.findAll(0, 20)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content[0].categoryName").isEqualTo("Electronics");
    }

    @Test
    void findByIdReturns404WhenMissing() {
        when(categoryService.findById(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Category not found with id 99")));

        webTestClient.get().uri("/api/v1/categories/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void createReturns201OnSuccess() {
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(Mono.just(response));

        webTestClient.post().uri("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Electronics"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.categoryName").isEqualTo("Electronics");
    }

    @Test
    void createReturns400OnValidationFailure() {
        webTestClient.post().uri("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest(""))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateReturns200OnSuccess() {
        CategoryResponse response = new CategoryResponse(1L, "Updated");
        when(categoryService.update(eq(1L), any(CategoryRequest.class))).thenReturn(Mono.just(response));

        webTestClient.put().uri("/api/v1/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Updated"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.categoryName").isEqualTo("Updated");
    }

    @Test
    void updateReturns404WhenMissing() {
        when(categoryService.update(eq(99L), any(CategoryRequest.class)))
                .thenReturn(Mono.error(new ResourceNotFoundException("Category not found with id 99")));

        webTestClient.put().uri("/api/v1/categories/99")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CategoryRequest("Updated"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteReturns422WhenCategoryStillHasProducts() {
        when(categoryService.delete(1L)).thenReturn(Mono.error(
                new BusinessRuleException("Cannot delete category 1 because it still has products")));

        webTestClient.delete().uri("/api/v1/categories/1")
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void deleteReturns200OnSuccess() {
        when(categoryService.delete(1L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/v1/categories/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
