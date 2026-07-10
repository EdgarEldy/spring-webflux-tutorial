package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import edgareldy.springwebfluxtutorial.entity.Product;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CategoryMapper;
import edgareldy.springwebfluxtutorial.mapper.ProductMapper;
import edgareldy.springwebfluxtutorial.repository.CategoryRepository;
import edgareldy.springwebfluxtutorial.repository.ProductRepository;
import edgareldy.springwebfluxtutorial.service.ProductService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of ProductService. Every method that returns a ProductResponse
 * resolves the linked Category via flatMap on CategoryRepository, since Product only stores a
 * raw categoryId and there is no automatic association to navigate.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
            ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Mono<ProductResponse> findById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id " + id)))
                .flatMap(this::toResponseWithCategory);
    }

    /**
     * Mono.zip runs the (optionally category-filtered) page query and its matching count query
     * concurrently, since R2DBC has no Page&lt;T&gt; that would carry both together.
     */
    @Override
    public Mono<PageResponse<ProductResponse>> findAll(int page, int size, Long categoryId) {
        long offset = (long) page * size;
        Flux<Product> productFlux = categoryId != null
                ? productRepository.findByCategoryIdPaged(categoryId, size, offset)
                : productRepository.findAllPaged(size, offset);
        Mono<Long> countMono = categoryId != null
                ? productRepository.countByCategoryId(categoryId)
                : productRepository.count();

        Mono<List<ProductResponse>> contentMono = productFlux
                .flatMap(this::toResponseWithCategory)
                .collectList();

        return Mono.zip(contentMono, countMono)
                .map(tuple -> PageResponse.of(tuple.getT1(), page, size, tuple.getT2()));
    }

    @Override
    @Transactional
    public Mono<ProductResponse> create(ProductRequest request) {
        return categoryRepository.findById(request.categoryId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Category not found with id " + request.categoryId())))
                .flatMap(category -> productRepository.save(productMapper.toEntity(request)))
                .flatMap(this::toResponseWithCategory);
    }

    @Override
    @Transactional
    public Mono<ProductResponse> update(Long id, ProductRequest request) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id " + id)))
                .flatMap(existing -> categoryRepository.findById(request.categoryId())
                        .switchIfEmpty(Mono.error(
                                new ResourceNotFoundException("Category not found with id " + request.categoryId())))
                        .flatMap(category -> {
                            existing.setCategoryId(request.categoryId());
                            existing.setProductName(request.productName());
                            existing.setUnitPrice(request.unitPrice());
                            return productRepository.save(existing);
                        }))
                .flatMap(this::toResponseWithCategory);
    }

    @Override
    @Transactional
    public Mono<Void> delete(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Product not found with id " + id)))
                .flatMap(existing -> productRepository.deleteById(id));
    }

    /**
     * Resolves the Category via flatMap instead of a blocking lookup, since Product has no
     * automatic association to navigate: this is the explicit relation resolution the README
     * calls out as the reactive replacement for JPA's ManyToOne.
     */
    private Mono<ProductResponse> toResponseWithCategory(Product product) {
        return categoryRepository.findById(product.getCategoryId())
                .map(categoryMapper::toResponse)
                .map((CategoryResponse categoryResponse) -> productMapper.toResponse(product, categoryResponse));
    }
}
