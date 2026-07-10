package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.entity.Category;
import edgareldy.springwebfluxtutorial.entity.Product;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CategoryMapper;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CategoryServiceImpl, in particular the delete() business rule (a category
 * still linked to products cannot be removed).
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, productRepository, categoryMapper);
    }

    @Test
    void findByIdReturnsMappedResponseWhenFound() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryRepository.findById(1L)).thenReturn(Mono.just(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        StepVerifier.create(categoryService.findById(1L))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void findByIdErrorsWithResourceNotFoundWhenMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(categoryService.findById(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void findAllZipsPagedContentWithTotalCount() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryRepository.findAllPaged(20, 0)).thenReturn(Flux.just(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);
        when(categoryRepository.count()).thenReturn(Mono.just(1L));

        StepVerifier.create(categoryService.findAll(0, 20))
                .expectNext(PageResponse.of(List.of(response), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void createSavesMappedEntityAndReturnsMappedResponse() {
        CategoryRequest request = new CategoryRequest("Electronics");
        Category toSave = Category.builder().categoryName("Electronics").build();
        Category saved = Category.builder().id(1L).categoryName("Electronics").build();
        CategoryResponse response = new CategoryResponse(1L, "Electronics");
        when(categoryMapper.toEntity(request)).thenReturn(toSave);
        when(categoryRepository.save(toSave)).thenReturn(Mono.just(saved));
        when(categoryMapper.toResponse(saved)).thenReturn(response);

        StepVerifier.create(categoryService.create(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void updateErrorsWithResourceNotFoundWhenMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(categoryService.update(1L, new CategoryRequest("New name")))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void updateSavesModifiedEntity() {
        Category existing = Category.builder().id(1L).categoryName("Old").build();
        Category updated = Category.builder().id(1L).categoryName("New").build();
        CategoryResponse response = new CategoryResponse(1L, "New");
        when(categoryRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(categoryRepository.save(existing)).thenReturn(Mono.just(updated));
        when(categoryMapper.toResponse(updated)).thenReturn(response);

        StepVerifier.create(categoryService.update(1L, new CategoryRequest("New")))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void deleteErrorsWithResourceNotFoundWhenMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(categoryService.delete(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void deleteErrorsWithBusinessRuleWhenProductsStillLinked() {
        Category existing = Category.builder().id(1L).categoryName("Electronics").build();
        Product linkedProduct = Product.builder().id(10L).categoryId(1L).build();
        when(categoryRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(productRepository.findByCategoryId(1L)).thenReturn(Flux.just(linkedProduct));

        StepVerifier.create(categoryService.delete(1L))
                .expectError(BusinessRuleException.class)
                .verify();
    }

    @Test
    void deletesWhenNoProductsLinked() {
        Category existing = Category.builder().id(1L).categoryName("Electronics").build();
        when(categoryRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(productRepository.findByCategoryId(1L)).thenReturn(Flux.empty());
        when(categoryRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(categoryService.delete(1L))
                .verifyComplete();

        verify(categoryRepository).deleteById(1L);
    }
}
