package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.entity.Order;
import java.time.Instant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for Order. The read queries join customers and products directly in SQL
 * (no JOIN FETCH available as in JPA), mapped to OrderView, since resolving those relations
 * per row with flatMap would mean one extra roundtrip per order on a paginated list.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {

    @Query("""
            SELECT o.id, o.customer_id, o.product_id, o.quantity, o.total,
                   c.first_name AS customer_first_name, c.last_name AS customer_last_name,
                   p.product_name AS product_name
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN products p ON o.product_id = p.id
            ORDER BY o.id LIMIT :limit OFFSET :offset
            """)
    Flux<OrderView> findAllWithDetails(long limit, long offset);

    @Query("""
            SELECT o.id, o.customer_id, o.product_id, o.quantity, o.total,
                   c.first_name AS customer_first_name, c.last_name AS customer_last_name,
                   p.product_name AS product_name
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN products p ON o.product_id = p.id
            WHERE o.id = :id
            """)
    Mono<OrderView> findByIdWithDetails(Long id);

    /**
     * Backs StockReportScheduler's periodic aggregation of the day's orders.
     */
    @Query("SELECT COUNT(*) FROM orders WHERE created_at >= :since")
    Mono<Long> countSince(Instant since);

    @Query("SELECT COALESCE(SUM(total), 0) FROM orders WHERE created_at >= :since")
    Mono<Double> sumTotalSince(Instant since);
}
