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
import edgareldy.springwebfluxtutorial.repository.OrderView;
import edgareldy.springwebfluxtutorial.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OrderServiceImpl, in particular that total is always computed from the
 * resolved Product's unit price (never trusted from the request) and that every write
 * publishes an OrderStatusEvent onto the shared sink.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private Sinks.Many<OrderStatusEvent> orderStatusSink;
    @Mock
    private AddressVerificationClient addressVerificationClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, customerRepository, productRepository,
                orderMapper, orderStatusSink, addressVerificationClient);
    }

    @Test
    void _01_ShouldReturnMappedResponse_WhenOrderIsFound() {
        OrderView view = new OrderView(1L, 2L, 3L, 2, 2000, "Ada", "Lovelace", "Laptop");
        OrderResponse response = response();
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Mono.just(view));
        when(orderMapper.toResponse(view)).thenReturn(response);

        StepVerifier.create(orderService.findById(1L))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void _02_ShouldErrorWithResourceNotFound_WhenOrderIsMissing() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.findById(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _03_ShouldComputeTotalFromProductUnitPrice_WhenOrderIsCreated() {
        OrderRequest request = new OrderRequest(2L, 3L, 4);
        Customer customer = Customer.builder().id(2L).firstName("Ada").lastName("Lovelace").address("1 Main St").build();
        Product product = Product.builder().id(3L).productName("Laptop").unitPrice(250f).build();
        Order saved = Order.builder().id(1L).customerId(2L).productId(3L).quantity(4).total(1000).build();
        OrderResponse response = response();

        when(customerRepository.findById(2L)).thenReturn(Mono.just(customer));
        when(productRepository.findById(3L)).thenReturn(Mono.just(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> Mono.just(saved));
        when(addressVerificationClient.verify(anyString())).thenReturn(Mono.just(true));
        when(orderMapper.toResponse(saved, customer, product)).thenReturn(response);
        when(orderStatusSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        StepVerifier.create(orderService.create(request))
                .expectNext(response)
                .verifyComplete();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotal()).isEqualTo(1000.0);

        ArgumentCaptor<OrderStatusEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusEvent.class);
        verify(orderStatusSink).tryEmitNext(eventCaptor.capture());
        assertThat(eventCaptor.getValue().status()).isEqualTo("CREATED");
        assertThat(eventCaptor.getValue().orderId()).isEqualTo(1L);
    }

    @Test
    void _04_ShouldErrorWithResourceNotFound_WhenOrderCustomerIsMissing() {
        OrderRequest request = new OrderRequest(2L, 3L, 4);
        when(customerRepository.findById(2L)).thenReturn(Mono.empty());
        when(productRepository.findById(3L)).thenReturn(Mono.just(Product.builder().id(3L).unitPrice(250f).build()));

        StepVerifier.create(orderService.create(request))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _05_ShouldErrorWithResourceNotFound_WhenOrderProductIsMissing() {
        OrderRequest request = new OrderRequest(2L, 3L, 4);
        when(customerRepository.findById(2L)).thenReturn(
                Mono.just(Customer.builder().id(2L).address("1 Main St").build()));
        when(productRepository.findById(3L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.create(request))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _06_ShouldZipPagedContentWithTotalCount_WhenAllOrdersAreRequested() {
        OrderView view = new OrderView(1L, 2L, 3L, 2, 2000, "Ada", "Lovelace", "Laptop");
        OrderResponse response = response();
        when(orderRepository.findAllWithDetails(20, 0)).thenReturn(Flux.just(view));
        when(orderRepository.count()).thenReturn(Mono.just(1L));
        when(orderMapper.toResponse(view)).thenReturn(response);

        StepVerifier.create(orderService.findAll(0, 20))
                .expectNext(PageResponse.of(List.of(response), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void _07_ShouldErrorWithResourceNotFound_WhenUpdatedOrderIsMissing() {
        when(orderRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.update(1L, new OrderRequest(2L, 3L, 4)))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _08_ShouldRecomputeTotalAndPublishUpdatedEvent_WhenOrderIsUpdated() {
        Order existing = Order.builder().id(1L).customerId(2L).productId(3L).quantity(1).total(1000).build();
        Customer customer = Customer.builder().id(9L).firstName("Grace").lastName("Hopper").build();
        Product product = Product.builder().id(8L).productName("Monitor").unitPrice(150f).build();
        Order updated = Order.builder().id(1L).customerId(9L).productId(8L).quantity(3).total(450).build();
        OrderResponse response = response();

        when(orderRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(customerRepository.findById(9L)).thenReturn(Mono.just(customer));
        when(productRepository.findById(8L)).thenReturn(Mono.just(product));
        when(orderRepository.save(existing)).thenReturn(Mono.just(updated));
        when(orderMapper.toResponse(updated, customer, product)).thenReturn(response);
        when(orderStatusSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        StepVerifier.create(orderService.update(1L, new OrderRequest(9L, 8L, 3)))
                .expectNext(response)
                .verifyComplete();

        assertThat(existing.getTotal()).isEqualTo(450.0);

        ArgumentCaptor<OrderStatusEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusEvent.class);
        verify(orderStatusSink).tryEmitNext(eventCaptor.capture());
        assertThat(eventCaptor.getValue().status()).isEqualTo("UPDATED");
    }

    @Test
    void _09_ShouldErrorWithResourceNotFound_WhenDeletedOrderIsMissing() {
        when(orderRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.delete(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _10_ShouldPublishDeletedEvent_WhenOrderIsDeleted() {
        Order existing = Order.builder().id(1L).customerId(2L).productId(3L).quantity(1).total(100).build();
        when(orderRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(orderRepository.deleteById(1L)).thenReturn(Mono.empty());
        when(orderStatusSink.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);

        StepVerifier.create(orderService.delete(1L))
                .verifyComplete();

        ArgumentCaptor<OrderStatusEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusEvent.class);
        verify(orderStatusSink).tryEmitNext(eventCaptor.capture());
        assertThat(eventCaptor.getValue().status()).isEqualTo("DELETED");
    }

    private OrderResponse response() {
        return new OrderResponse(1L, new OrderResponse.CustomerSummary(2L, "Ada", "Lovelace"),
                new OrderResponse.ProductSummary(3L, "Laptop"), 2, 2000);
    }
}
