package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.TestcontainersConfiguration;
import edgareldy.springwebfluxtutorial.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

/**
 * Integration test for CategoryRepository against a real PostgreSQL container (Testcontainers),
 * verifying the custom findAllPaged() query and basic CRUD wiring. FlywayAutoConfiguration is
 * imported explicitly since @DataR2dbcTest does not pull it in by default, and the schema must
 * exist before any query runs.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Deletes orders and products first: this test class shares its Spring context (and
     * Testcontainers instance) with OrderRepositoryTest/ProductRepositoryTest via context
     * caching, since all three use the exact same @DataR2dbcTest configuration, so leftover
     * rows from another test class can still reference a category this class is about to
     * delete.
     */
    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll().block();
        productRepository.deleteAll().block();
        categoryRepository.deleteAll().block();
    }

    @Test
    void _01_ShouldSaveAndFindCategory_WhenFindingById() {
        Category saved = categoryRepository.save(Category.builder().categoryName("Electronics").build()).block();

        StepVerifier.create(categoryRepository.findById(saved.getId()))
                .expectNextMatches(found -> found.getCategoryName().equals("Electronics"))
                .verifyComplete();
    }

    @Test
    void _02_ShouldOrderByIdAndRespectLimitOffset_WhenFindingAllPaged() {
        categoryRepository.save(Category.builder().categoryName("A").build()).block();
        categoryRepository.save(Category.builder().categoryName("B").build()).block();
        categoryRepository.save(Category.builder().categoryName("C").build()).block();

        StepVerifier.create(categoryRepository.findAllPaged(2, 1))
                .expectNextMatches(category -> category.getCategoryName().equals("B"))
                .expectNextMatches(category -> category.getCategoryName().equals("C"))
                .verifyComplete();
    }

    @Test
    void _03_ShouldReflectNumberOfRows_WhenCounting() {
        categoryRepository.save(Category.builder().categoryName("A").build()).block();
        categoryRepository.save(Category.builder().categoryName("B").build()).block();

        StepVerifier.create(categoryRepository.count())
                .expectNext(2L)
                .verifyComplete();
    }
}
