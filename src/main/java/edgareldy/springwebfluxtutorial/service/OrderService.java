package edgareldy.springwebfluxtutorial.service;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderRequest;
import edgareldy.springwebfluxtutorial.dto.order.OrderResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive contract for Order business operations. OrderController depends on this interface,
 * never on OrderServiceImpl directly.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public interface OrderService {

    Mono<OrderResponse> findById(Long id);

    Mono<PageResponse<OrderResponse>> findAll(int page, int size);

    Mono<OrderResponse> create(OrderRequest request);

    Mono<OrderResponse> update(Long id, OrderRequest request);

    Mono<Void> delete(Long id);
}
