package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.TestcontainersConfiguration;
import edgareldy.springwebfluxtutorial.entity.Category;
import edgareldy.springwebfluxtutorial.entity.Customer;
import edgareldy.springwebfluxtutorial.entity.Order;
import edgareldy.springwebfluxtutorial.entity.Product;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

/**
 * Integration test for OrderRepository against a real PostgreSQL container (Testcontainers),
 * verifying the SQL join projection (OrderView) and the createdAt-based aggregation queries
 * StockReportScheduler relies on.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class OrderRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private OrderRepository orderRepository;

    private Long customerId;
    private Long productId;

    @BeforeEach
    void seedReferenceData() {
        orderRepository.deleteAll().block();
        productRepository.deleteAll().block();
        categoryRepository.deleteAll().block();
        customerRepository.deleteAll().block();

        Long categoryId = categoryRepository.save(Category.builder().categoryName("Electronics").build())
                .block()
                .getId();
        productId = productRepository.save(Product.builder()
                        .categoryId(categoryId)
                        .productName("Laptop")
                        .unitPrice(1000f)
                        .build())
                .block()
                .getId();
        customerId = customerRepository.save(Customer.builder()
                        .firstName("Ada")
                        .lastName("Lovelace")
                        .telephone("555-0100")
                        .email("ada@example.com")
                        .address("1 Main St")
                        .build())
                .block()
                .getId();
    }

    @Test
    void findByIdWithDetailsJoinsCustomerAndProduct() {
        Order saved = orderRepository.save(Order.builder()
                        .customerId(customerId)
                        .productId(productId)
                        .quantity(2)
                        .total(2000)
                        .build())
                .block();

        StepVerifier.create(orderRepository.findByIdWithDetails(saved.getId()))
                .expectNextMatches(view -> view.customerFirstName().equals("Ada")
                        && view.productName().equals("Laptop")
                        && view.total() == 2000)
                .verifyComplete();
    }

    @Test
    void findAllWithDetailsOrdersByIdAndRespectsLimitOffset() {
        orderRepository.save(Order.builder().customerId(customerId).productId(productId).quantity(1).total(1000).build()).block();
        orderRepository.save(Order.builder().customerId(customerId).productId(productId).quantity(2).total(2000).build()).block();
        orderRepository.save(Order.builder().customerId(customerId).productId(productId).quantity(3).total(3000).build()).block();

        StepVerifier.create(orderRepository.findAllWithDetails(2, 1))
                .expectNextMatches(view -> view.quantity() == 2)
                .expectNextMatches(view -> view.quantity() == 3)
                .verifyComplete();
    }

    @Test
    void countSinceAndSumTotalSinceAggregateTodaysOrders() {
        orderRepository.save(Order.builder().customerId(customerId).productId(productId).quantity(1).total(1000).build()).block();
        orderRepository.save(Order.builder().customerId(customerId).productId(productId).quantity(2).total(2000).build()).block();

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        StepVerifier.create(orderRepository.countSince(startOfDay))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(orderRepository.sumTotalSince(startOfDay))
                .expectNext(3000.0)
                .verifyComplete();
    }
}
