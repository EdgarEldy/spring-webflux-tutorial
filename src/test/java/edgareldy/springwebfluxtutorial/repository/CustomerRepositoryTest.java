package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.TestcontainersConfiguration;
import edgareldy.springwebfluxtutorial.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

/**
 * Integration test for CustomerRepository against a real PostgreSQL container
 * (Testcontainers), verifying case-insensitive email lookup and the name search query.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Deletes orders first: this test class shares its Spring context (and Testcontainers
     * instance) with OrderRepositoryTest via context caching, since both use the exact same
     * @DataR2dbcTest configuration, so leftover orders from another test class can still
     * reference a customer this class is about to delete.
     */
    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll().block();
        customerRepository.deleteAll().block();
    }

    @Test
    void _01_ShouldMatchRegardlessOfCase_WhenFindingByEmail() {
        customerRepository.save(customer("Ada", "Lovelace", "ada@example.com")).block();

        StepVerifier.create(customerRepository.findByEmailIgnoreCase("ADA@EXAMPLE.COM"))
                .expectNextMatches(found -> found.getFirstName().equals("Ada"))
                .verifyComplete();
    }

    @Test
    void _02_ShouldMatchFirstOrLastName_WhenSearchingPaged() {
        customerRepository.save(customer("Ada", "Lovelace", "ada@example.com")).block();
        customerRepository.save(customer("Grace", "Hopper", "grace@example.com")).block();

        StepVerifier.create(customerRepository.searchPaged("lovelace", 20, 0))
                .expectNextMatches(found -> found.getFirstName().equals("Ada"))
                .verifyComplete();

        StepVerifier.create(customerRepository.countSearch("lovelace"))
                .expectNext(1L)
                .verifyComplete();
    }

    private Customer customer(String firstName, String lastName, String email) {
        return Customer.builder()
                .firstName(firstName)
                .lastName(lastName)
                .telephone("555-0100")
                .email(email)
                .address("1 Main St")
                .build();
    }
}
