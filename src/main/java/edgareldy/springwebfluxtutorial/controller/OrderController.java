package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderRequest;
import edgareldy.springwebfluxtutorial.dto.order.OrderResponse;
import edgareldy.springwebfluxtutorial.dto.order.OrderStatusEvent;
import edgareldy.springwebfluxtutorial.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Annotated REST controller for Order, delegating all business logic to OrderService. Every
 * response is wrapped in Mono&lt;ApiResponse&lt;T&gt;&gt;, except /stream, which exposes the
 * shared Sinks.Many as a raw Flux&lt;OrderStatusEvent&gt;: the text/event-stream protocol
 * frames events itself, so wrapping would only get in the way.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final Sinks.Many<OrderStatusEvent> orderStatusSink;

    public OrderController(OrderService orderService, Sinks.Many<OrderStatusEvent> orderStatusSink) {
        this.orderService = orderService;
        this.orderStatusSink = orderStatusSink;
    }

    @GetMapping
    public Mono<ApiResponse<PageResponse<OrderResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.findAll(page, size)
                .map(result -> ApiResponse.success(result, "Orders retrieved"));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<OrderResponse>> findById(@PathVariable Long id) {
        return orderService.findById(id)
                .map(result -> ApiResponse.success(result, "Order retrieved"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<OrderResponse>> create(@Valid @RequestBody OrderRequest request) {
        return orderService.create(request)
                .map(result -> ApiResponse.success(result, "Order created"));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<OrderResponse>> update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.update(id, request)
                .map(result -> ApiResponse.success(result, "Order updated"));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        return orderService.delete(id)
                .thenReturn(ApiResponse.<Void>success(null, "Order deleted"));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderStatusEvent> stream() {
        return orderStatusSink.asFlux();
    }
}
