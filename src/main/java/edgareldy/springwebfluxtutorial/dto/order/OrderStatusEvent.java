package edgareldy.springwebfluxtutorial.dto.order;

import java.time.Instant;

/**
 * Event published on the shared Sinks.Many bean whenever an order is created, updated or
 * deleted, streamed raw (not wrapped in ApiResponse) to every subscriber of
 * GET /api/v1/orders/stream.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record OrderStatusEvent(
        Long orderId,
        String status,
        Instant occurredAt
) {
}
