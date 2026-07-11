package edgareldy.springwebfluxtutorial.config;

import edgareldy.springwebfluxtutorial.dto.order.OrderStatusEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

/**
 * Declares the shared hot, multicast Sinks.Many bean order creation/update/deletion events are
 * published to, and every GET /api/v1/orders/stream subscriber reads from. Unlike the cold
 * repository/WebClient publishers used elsewhere in this project, every currently subscribed
 * client receives the same live events as they happen rather than each triggering its own
 * independent stream.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Configuration
public class SseConfig {

    @Bean
    public Sinks.Many<OrderStatusEvent> orderStatusSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }
}
