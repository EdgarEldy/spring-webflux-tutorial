package edgareldy.springwebfluxtutorial.service;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive contract for Product business operations. Controllers depend on this interface,
 * never on ProductServiceImpl directly.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface ProductService {

    Mono<ProductResponse> findById(Long id);

    Mono<PageResponse<ProductResponse>> findAll(int page, int size, Long categoryId);

    Mono<ProductResponse> create(ProductRequest request);

    Mono<ProductResponse> update(Long id, ProductRequest request);

    Mono<Void> delete(Long id);
}
