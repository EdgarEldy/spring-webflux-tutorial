package edgareldy.springwebfluxtutorial.scheduler;

import edgareldy.springwebfluxtutorial.repository.OrderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Periodically aggregates the day's orders and logs a report. @Scheduled methods are plain
 * void callbacks, not part of any reactive chain a caller subscribes to, so subscribing here
 * with subscribe() is the correct way to trigger the pipeline: there is no downstream Mono to
 * return it to, unlike a controller or handler method.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Component
public class StockReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockReportScheduler.class);

    private final OrderRepository orderRepository;

    public StockReportScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void reportDailyOrders() {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        Mono.zip(orderRepository.countSince(startOfDay), orderRepository.sumTotalSince(startOfDay))
                .subscribe(
                        counts -> log.info("Daily order report: {} orders, {} total revenue",
                                counts.getT1(), counts.getT2()),
                        ex -> log.error("Daily order report failed", ex));
    }
}
