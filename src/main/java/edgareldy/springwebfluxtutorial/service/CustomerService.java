package edgareldy.springwebfluxtutorial.service;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerRequest;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive contract for Customer business operations. CustomerHandler depends on this
 * interface, never on CustomerServiceImpl directly.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface CustomerService {

    Mono<CustomerResponse> findById(Long id);

    Mono<PageResponse<CustomerResponse>> findAll(int page, int size, String search);

    Mono<CustomerResponse> create(CustomerRequest request);

    Mono<CustomerResponse> update(Long id, CustomerRequest request);

    Mono<Void> delete(Long id);
}
