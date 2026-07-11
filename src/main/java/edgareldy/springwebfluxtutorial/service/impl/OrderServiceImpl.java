package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.client.AddressVerificationClient;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderRequest;
import edgareldy.springwebfluxtutorial.dto.order.OrderResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderStatusEvent;
import edgareldy.springwebfluxtutorial.entity.Customer;
import edgareldy.springwebfluxtutorial.entity.Order;
import edgareldy.springwebfluxtutorial.entity.Product;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.OrderMapper;
import edgareldy.springwebfluxtutorial.repository.CustomerRepository;
import edgareldy.springwebfluxtutorial.repository.OrderRepository;
import edgareldy.springwebfluxtutorial.repository.ProductRepository;
import edgareldy.springwebfluxtutorial.service.OrderService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Reactive implementation of OrderService. Every write publishes an OrderStatusEvent onto the
 * shared Sinks.Many bean, so any client subscribed to GET /api/v1/orders/stream sees it live.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final Sinks.Many<OrderStatusEvent> orderStatusSink;
    private final AddressVerificationClient addressVerificationClient;

    public OrderServiceImpl(OrderRepository orderRepository, CustomerRepository customerRepository,
            ProductRepository productRepository, OrderMapper orderMapper,
            Sinks.Many<OrderStatusEvent> orderStatusSink, AddressVerificationClient addressVerificationClient) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.orderStatusSink = orderStatusSink;
        this.addressVerificationClient = addressVerificationClient;
    }

    @Override
    public Mono<OrderResponse> findById(Long id) {
        return orderRepository.findByIdWithDetails(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order not found with id " + id)))
                .map(orderMapper::toResponse);
    }

    /**
     * Mono.zip runs the joined page query and its count query concurrently, since R2DBC has
     * no Page&lt;T&gt; that would carry both together.
     */
    @Override
    public Mono<PageResponse<OrderResponse>> findAll(int page, int size) {
        long offset = (long) page * size;
        Mono<List<OrderResponse>> contentMono = orderRepository.findAllWithDetails(size, offset)
                .map(orderMapper::toResponse)
                .collectList();

        return Mono.zip(contentMono, orderRepository.count())
                .map(tuple -> PageResponse.of(tuple.getT1(), page, size, tuple.getT2()));
    }

    /**
     * Customer and Product are resolved concurrently via Mono.zip, since neither lookup
     * depends on the other's result. total is computed from the resolved Product's unit
     * price, never trusted from the request. Address verification runs after the save so a
     * slow or failing external call never blocks persisting the order.
     */
    @Override
    @Transactional
    public Mono<OrderResponse> create(OrderRequest request) {
        Mono<Customer> customerMono = customerRepository.findById(request.customerId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Customer not found with id " + request.customerId())));
        Mono<Product> productMono = productRepository.findById(request.productId())
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Product not found with id " + request.productId())));

        return Mono.zip(customerMono, productMono)
                .flatMap(resolved -> {
                    Customer customer = resolved.getT1();
                    Product product = resolved.getT2();
                    Order order = Order.builder()
                            .customerId(customer.getId())
                            .productId(product.getId())
                            .quantity(request.quantity())
                            .total(product.getUnitPrice() * request.quantity())
                            .build();

                    return orderRepository.save(order)
                            .doOnNext(saved -> publishStatusEvent(saved.getId(), "CREATED"))
                            .flatMap(saved -> addressVerificationClient.verify(customer.getAddress())
                                    .doOnNext(verified -> log.info("Address verification for order {}: {}",
                                            saved.getId(), verified))
                                    .thenReturn(saved))
                            .map(saved -> orderMapper.toResponse(saved, customer, product));
                });
    }

    @Override
    @Transactional
    public Mono<OrderResponse> update(Long id, OrderRequest request) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order not found with id " + id)))
                .flatMap(existing -> {
                    Mono<Customer> customerMono = customerRepository.findById(request.customerId())
                            .switchIfEmpty(Mono.error(
                                    new ResourceNotFoundException("Customer not found with id " + request.customerId())));
                    Mono<Product> productMono = productRepository.findById(request.productId())
                            .switchIfEmpty(Mono.error(
                                    new ResourceNotFoundException("Product not found with id " + request.productId())));

                    return Mono.zip(customerMono, productMono)
                            .flatMap(resolved -> {
                                Customer customer = resolved.getT1();
                                Product product = resolved.getT2();
                                existing.setCustomerId(customer.getId());
                                existing.setProductId(product.getId());
                                existing.setQuantity(request.quantity());
                                existing.setTotal(product.getUnitPrice() * request.quantity());
                                return orderRepository.save(existing)
                                        .doOnNext(saved -> publishStatusEvent(saved.getId(), "UPDATED"))
                                        .map(saved -> orderMapper.toResponse(saved, customer, product));
                            });
                });
    }

    @Override
    @Transactional
    public Mono<Void> delete(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order not found with id " + id)))
                .flatMap(existing -> orderRepository.deleteById(id)
                        .doOnSuccess(deleted -> publishStatusEvent(id, "DELETED")));
    }

    private void publishStatusEvent(Long orderId, String status) {
        Sinks.EmitResult result = orderStatusSink.tryEmitNext(new OrderStatusEvent(orderId, status, Instant.now()));
        if (result.isFailure()) {
            log.warn("Failed to publish order status event for order {} ({}): {}", orderId, status, result);
        }
    }
}
