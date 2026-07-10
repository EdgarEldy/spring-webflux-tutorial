package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Annotated REST controller for Category, delegating all business logic to CategoryService.
 * Every response is wrapped in Mono&lt;ApiResponse&lt;T&gt;&gt;, per this project's standard
 * response format.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Mono<ApiResponse<PageResponse<CategoryResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return categoryService.findAll(page, size)
                .map(result -> ApiResponse.success(result, "Categories retrieved"));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        return categoryService.findById(id)
                .map(result -> ApiResponse.success(result, "Category retrieved"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request)
                .map(result -> ApiResponse.success(result, "Category created"));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<CategoryResponse>> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request)
                .map(result -> ApiResponse.success(result, "Category updated"));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        return categoryService.delete(id)
                .thenReturn(ApiResponse.<Void>success(null, "Category deleted"));
    }
}
