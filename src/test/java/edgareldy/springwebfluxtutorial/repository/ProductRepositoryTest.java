package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.TestcontainersConfiguration;
import edgareldy.springwebfluxtutorial.entity.Category;
import edgareldy.springwebfluxtutorial.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

/**
 * Integration test for ProductRepository against a real PostgreSQL container (Testcontainers),
 * verifying category filtering, pagination, and the findByCategoryId() query
 * CategoryServiceImpl.delete() relies on to enforce its business rule.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long categoryId;

    /**
     * Deletes orders first: this test class shares its Spring context (and Testcontainers
     * instance) with OrderRepositoryTest via context caching, since both use the exact same
     * @DataR2dbcTest configuration, so leftover orders from another test class can still
     * reference a product this class is about to delete.
     */
    @BeforeEach
    void cleanDatabaseAndSeedCategory() {
        orderRepository.deleteAll().block();
        productRepository.deleteAll().block();
        categoryRepository.deleteAll().block();
        categoryId = categoryRepository.save(Category.builder().categoryName("Electronics").build())
                .block()
                .getId();
    }

    @Test
    void findByCategoryIdReturnsOnlyProductsOfThatCategory() {
        Long otherCategoryId = categoryRepository.save(Category.builder().categoryName("Books").build())
                .block()
                .getId();
        productRepository.save(Product.builder().categoryId(categoryId).productName("Laptop").unitPrice(999f).build())
                .block();
        productRepository.save(Product.builder().categoryId(otherCategoryId).productName("Novel").unitPrice(15f).build())
                .block();

        StepVerifier.create(productRepository.findByCategoryId(categoryId))
                .expectNextMatches(product -> product.getProductName().equals("Laptop"))
                .verifyComplete();
    }

    @Test
    void findAllPagedOrdersByIdAndRespectsLimitOffset() {
        productRepository.save(Product.builder().categoryId(categoryId).productName("A").unitPrice(1f).build()).block();
        productRepository.save(Product.builder().categoryId(categoryId).productName("B").unitPrice(2f).build()).block();
        productRepository.save(Product.builder().categoryId(categoryId).productName("C").unitPrice(3f).build()).block();

        StepVerifier.create(productRepository.findAllPaged(2, 1))
                .expectNextMatches(product -> product.getProductName().equals("B"))
                .expectNextMatches(product -> product.getProductName().equals("C"))
                .verifyComplete();
    }

    @Test
    void countByCategoryIdCountsOnlyThatCategory() {
        Long otherCategoryId = categoryRepository.save(Category.builder().categoryName("Books").build())
                .block()
                .getId();
        productRepository.save(Product.builder().categoryId(categoryId).productName("Laptop").unitPrice(999f).build())
                .block();
        productRepository.save(Product.builder().categoryId(otherCategoryId).productName("Novel").unitPrice(15f).build())
                .block();

        StepVerifier.create(productRepository.countByCategoryId(categoryId))
                .expectNext(1L)
                .verifyComplete();
    }
}
