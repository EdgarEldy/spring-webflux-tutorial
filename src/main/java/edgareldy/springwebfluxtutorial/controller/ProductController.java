package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import edgareldy.springwebfluxtutorial.service.ProductService;
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
 * Annotated REST controller for Product, delegating all business logic to ProductService.
 * Every response is wrapped in Mono&lt;ApiResponse&lt;T&gt;&gt;, per this project's standard
 * response format.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Mono<ApiResponse<PageResponse<ProductResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId) {
        return productService.findAll(page, size, categoryId)
                .map(result -> ApiResponse.success(result, "Products retrieved"));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(result -> ApiResponse.success(result, "Product retrieved"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request)
                .map(result -> ApiResponse.success(result, "Product created"));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<ProductResponse>> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request)
                .map(result -> ApiResponse.success(result, "Product updated"));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        return productService.delete(id)
                .thenReturn(ApiResponse.<Void>success(null, "Product deleted"));
    }
}
