package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderRequest;
import edgareldy.springwebfluxtutorial.dto.order.OrderResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderStatusEvent;
import edgareldy.springwebfluxtutorial.exception.GlobalExceptionHandler;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.service.OrderService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * WebTestClient tests for OrderController, covering the nominal path, the 400/404 cases
 * GlobalExceptionHandler handles, and the SSE stream endpoint. Security auto-configuration is
 * excluded since no SecurityConfig exists yet on this branch.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@WebFluxTest(controllers = OrderController.class,
        excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private Sinks.Many<OrderStatusEvent> orderStatusSink;

    @Test
    void _01_ShouldReturnWrappedPage_WhenOrdersAreListed() {
        OrderResponse order = orderResponse();
        PageResponse<OrderResponse> page = PageResponse.of(List.of(order), 0, 20, 1);
        when(orderService.findAll(0, 20)).thenReturn(Mono.just(page));

        webTestClient.get().uri("/api/v1/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content[0].product.productName").isEqualTo("Laptop");
    }

    @Test
    void _02_ShouldReturn404_WhenOrderIsMissing() {
        when(orderService.findById(99L))
                .thenReturn(Mono.error(new ResourceNotFoundException("Order not found with id 99")));

        webTestClient.get().uri("/api/v1/orders/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void _03_ShouldReturn201_WhenOrderIsCreated() {
        when(orderService.create(any(OrderRequest.class))).thenReturn(Mono.just(orderResponse()));

        webTestClient.post().uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OrderRequest(2L, 3L, 2))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(2000);
    }

    @Test
    void _04_ShouldReturn400_WhenValidationFails() {
        webTestClient.post().uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OrderRequest(null, null, -1))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void _05_ShouldReturn200_WhenOrderIsUpdated() {
        when(orderService.update(eq(1L), any(OrderRequest.class)))
                .thenReturn(Mono.just(orderResponse()));

        webTestClient.put().uri("/api/v1/orders/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OrderRequest(2L, 3L, 2))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void _06_ShouldReturn200_WhenOrderIsDeleted() {
        when(orderService.delete(1L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/v1/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    @Test
    void _07_ShouldExposeRawEventStreamNotWrappedInApiResponse_WhenStreamIsRequested() {
        OrderStatusEvent event = new OrderStatusEvent(1L, "CREATED", Instant.now());
        when(orderStatusSink.asFlux()).thenReturn(Flux.just(event));

        webTestClient.get().uri("/api/v1/orders/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(OrderStatusEvent.class)
                .hasSize(1)
                .contains(event);
    }

    private OrderResponse orderResponse() {
        return new OrderResponse(1L, new OrderResponse.CustomerSummary(2L, "Ada", "Lovelace"),
                new OrderResponse.ProductSummary(3L, "Laptop"), 2, 2000);
    }
}
