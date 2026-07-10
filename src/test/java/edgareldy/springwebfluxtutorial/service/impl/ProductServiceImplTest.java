package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import edgareldy.springwebfluxtutorial.entity.Category;
import edgareldy.springwebfluxtutorial.entity.Product;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CategoryMapper;
import edgareldy.springwebfluxtutorial.mapper.ProductMapper;
import edgareldy.springwebfluxtutorial.repository.CategoryRepository;
import edgareldy.springwebfluxtutorial.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

/**
 * Unit tests for ProductServiceImpl, in particular the Category resolution flatMap and the
 * check that a Product cannot be created against a non-existent Category.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryMapper categoryMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, categoryRepository, productMapper, categoryMapper);
    }

    @Test
    void findByIdResolvesCategoryAndReturnsMappedResponse() {
        Product product = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        Category category = Category.builder().id(2L).categoryName("Electronics").build();
        CategoryResponse categoryResponse = new CategoryResponse(2L, "Electronics");
        ProductResponse productResponse = new ProductResponse(1L, categoryResponse, "Laptop", 999f);

        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(categoryRepository.findById(2L)).thenReturn(Mono.just(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
        when(productMapper.toResponse(product, categoryResponse)).thenReturn(productResponse);

        StepVerifier.create(productService.findById(1L))
                .expectNext(productResponse)
                .verifyComplete();
    }

    @Test
    void findByIdErrorsWithResourceNotFoundWhenMissing() {
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.findById(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void createErrorsWithResourceNotFoundWhenCategoryMissing() {
        ProductRequest request = new ProductRequest(2L, "Laptop", 999f);
        when(categoryRepository.findById(2L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.create(request))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void createSavesProductWhenCategoryExists() {
        ProductRequest request = new ProductRequest(2L, "Laptop", 999f);
        Category category = Category.builder().id(2L).categoryName("Electronics").build();
        Product toSave = Product.builder().categoryId(2L).productName("Laptop").unitPrice(999f).build();
        Product saved = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        CategoryResponse categoryResponse = new CategoryResponse(2L, "Electronics");
        ProductResponse productResponse = new ProductResponse(1L, categoryResponse, "Laptop", 999f);

        when(categoryRepository.findById(2L)).thenReturn(Mono.just(category));
        when(productMapper.toEntity(request)).thenReturn(toSave);
        when(productRepository.save(toSave)).thenReturn(Mono.just(saved));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
        when(productMapper.toResponse(saved, categoryResponse)).thenReturn(productResponse);

        StepVerifier.create(productService.create(request))
                .expectNext(productResponse)
                .verifyComplete();
    }

    @Test
    void updateErrorsWithResourceNotFoundWhenProductMissing() {
        ProductRequest request = new ProductRequest(2L, "Laptop", 999f);
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.update(1L, request))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void updateErrorsWithResourceNotFoundWhenCategoryMissing() {
        ProductRequest request = new ProductRequest(99L, "Laptop", 999f);
        Product existing = Product.builder().id(1L).categoryId(2L).productName("Old").unitPrice(1f).build();
        when(productRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(categoryRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.update(1L, request))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void updateSavesProductWhenCategoryExists() {
        ProductRequest request = new ProductRequest(3L, "Laptop Pro", 1299f);
        Product existing = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        Category newCategory = Category.builder().id(3L).categoryName("Premium").build();
        Product updated = Product.builder().id(1L).categoryId(3L).productName("Laptop Pro").unitPrice(1299f).build();
        CategoryResponse categoryResponse = new CategoryResponse(3L, "Premium");
        ProductResponse productResponse = new ProductResponse(1L, categoryResponse, "Laptop Pro", 1299f);

        when(productRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(categoryRepository.findById(3L)).thenReturn(Mono.just(newCategory));
        when(productRepository.save(existing)).thenReturn(Mono.just(updated));
        when(categoryMapper.toResponse(newCategory)).thenReturn(categoryResponse);
        when(productMapper.toResponse(updated, categoryResponse)).thenReturn(productResponse);

        StepVerifier.create(productService.update(1L, request))
                .expectNext(productResponse)
                .verifyComplete();
    }

    @Test
    void findAllZipsPagedContentWithTotalCount() {
        Product product = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        Category category = Category.builder().id(2L).categoryName("Electronics").build();
        CategoryResponse categoryResponse = new CategoryResponse(2L, "Electronics");
        ProductResponse productResponse = new ProductResponse(1L, categoryResponse, "Laptop", 999f);

        when(productRepository.findAllPaged(20, 0)).thenReturn(Flux.just(product));
        when(productRepository.count()).thenReturn(Mono.just(1L));
        when(categoryRepository.findById(2L)).thenReturn(Mono.just(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
        when(productMapper.toResponse(product, categoryResponse)).thenReturn(productResponse);

        StepVerifier.create(productService.findAll(0, 20, null))
                .expectNext(PageResponse.of(List.of(productResponse), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void findAllFiltersByCategoryIdWhenProvided() {
        Product product = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        Category category = Category.builder().id(2L).categoryName("Electronics").build();
        CategoryResponse categoryResponse = new CategoryResponse(2L, "Electronics");
        ProductResponse productResponse = new ProductResponse(1L, categoryResponse, "Laptop", 999f);

        when(productRepository.findByCategoryIdPaged(2L, 20, 0)).thenReturn(Flux.just(product));
        when(productRepository.countByCategoryId(2L)).thenReturn(Mono.just(1L));
        when(categoryRepository.findById(2L)).thenReturn(Mono.just(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);
        when(productMapper.toResponse(product, categoryResponse)).thenReturn(productResponse);

        StepVerifier.create(productService.findAll(0, 20, 2L))
                .expectNext(PageResponse.of(List.of(productResponse), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void deleteErrorsWithResourceNotFoundWhenMissing() {
        when(productRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void deletesWhenFound() {
        Product product = Product.builder().id(1L).categoryId(2L).productName("Laptop").unitPrice(999f).build();
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(productService.delete(1L))
                .verifyComplete();
    }
}
