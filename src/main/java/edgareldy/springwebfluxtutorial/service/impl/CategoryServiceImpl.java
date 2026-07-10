package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.entity.Category;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CategoryMapper;
import edgareldy.springwebfluxtutorial.repository.CategoryRepository;
import edgareldy.springwebfluxtutorial.repository.ProductRepository;
import edgareldy.springwebfluxtutorial.service.CategoryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of CategoryService. The delete() business rule (a category still
 * linked to products cannot be removed) is checked via Flux.hasElements() on ProductRepository
 * rather than loading products into a blocking collection just to test emptiness.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository,
            CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Mono<CategoryResponse> findById(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found with id " + id)))
                .map(categoryMapper::toResponse);
    }

    /**
     * Mono.zip runs the page query and the count query concurrently and waits for both, since
     * R2DBC has no Page&lt;T&gt; that would carry the total count alongside the content.
     */
    @Override
    public Mono<PageResponse<CategoryResponse>> findAll(int page, int size) {
        long offset = (long) page * size;
        Mono<List<CategoryResponse>> contentMono = categoryRepository.findAllPaged(size, offset)
                .map(categoryMapper::toResponse)
                .collectList();

        return Mono.zip(contentMono, categoryRepository.count())
                .map(tuple -> PageResponse.of(tuple.getT1(), page, size, tuple.getT2()));
    }

    @Override
    @Transactional
    public Mono<CategoryResponse> create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        return categoryRepository.save(category)
                .map(categoryMapper::toResponse);
    }

    @Override
    @Transactional
    public Mono<CategoryResponse> update(Long id, CategoryRequest request) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found with id " + id)))
                .flatMap(existing -> {
                    existing.setCategoryName(request.categoryName());
                    return categoryRepository.save(existing);
                })
                .map(categoryMapper::toResponse);
    }

    /**
     * hasElements() on the Flux checks emptiness reactively (completes with true/false as soon
     * as one row arrives or the stream ends) instead of collecting every linked product into a
     * blocking list just to test its size.
     */
    @Override
    @Transactional
    public Mono<Void> delete(Long id) {
        return categoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found with id " + id)))
                .flatMap(existing -> productRepository.findByCategoryId(id).hasElements())
                .flatMap(hasProducts -> {
                    if (hasProducts) {
                        return Mono.error(new BusinessRuleException(
                                "Cannot delete category " + id + " because it still has products"));
                    }
                    return categoryRepository.deleteById(id);
                });
    }
}
