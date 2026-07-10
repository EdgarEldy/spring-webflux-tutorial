package edgareldy.springwebfluxtutorial.service;

import edgareldy.springwebfluxtutorial.dto.category.CategoryRequest;
import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive contract for Category business operations. Controllers depend on this interface,
 * never on CategoryServiceImpl directly.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface CategoryService {

    Mono<CategoryResponse> findById(Long id);

    Mono<PageResponse<CategoryResponse>> findAll(int page, int size);

    Mono<CategoryResponse> create(CategoryRequest request);

    Mono<CategoryResponse> update(Long id, CategoryRequest request);

    Mono<Void> delete(Long id);
}
