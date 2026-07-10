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

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll().block();
    }

    @Test
    void savesAndFindsById() {
        Category saved = categoryRepository.save(Category.builder().categoryName("Electronics").build()).block();

        StepVerifier.create(categoryRepository.findById(saved.getId()))
                .expectNextMatches(found -> found.getCategoryName().equals("Electronics"))
                .verifyComplete();
    }

    @Test
    void findAllPagedOrdersByIdAndRespectsLimitOffset() {
        categoryRepository.save(Category.builder().categoryName("A").build()).block();
        categoryRepository.save(Category.builder().categoryName("B").build()).block();
        categoryRepository.save(Category.builder().categoryName("C").build()).block();

        StepVerifier.create(categoryRepository.findAllPaged(2, 1))
                .expectNextMatches(category -> category.getCategoryName().equals("B"))
                .expectNextMatches(category -> category.getCategoryName().equals("C"))
                .verifyComplete();
    }

    @Test
    void countReflectsNumberOfRows() {
        categoryRepository.save(Category.builder().categoryName("A").build()).block();
        categoryRepository.save(Category.builder().categoryName("B").build()).block();

        StepVerifier.create(categoryRepository.count())
                .expectNext(2L)
                .verifyComplete();
    }
}
