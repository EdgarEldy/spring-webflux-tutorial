package edgareldy.springwebfluxtutorial.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping the orders table. Holds only raw customerId/productId foreign keys, no
 * automatic association to Customer or Product: OrderServiceImpl resolves both explicitly via
 * Mono.zip when creating an order, and OrderRepository joins them at the SQL level for reads.
 * createdAt is populated automatically by R2dbcConfig's @EnableR2dbcAuditing.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Table("orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    private Long id;

    @Column("customer_id")
    private Long customerId;

    @Column("product_id")
    private Long productId;

    private int quantity;

    private double total;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
