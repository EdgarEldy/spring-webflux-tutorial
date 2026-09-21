package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.TestcontainersConfiguration;
import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

/**
 * Integration test for AppUserRepository against a real PostgreSQL container (Testcontainers),
 * verifying that Role, a Java enum, round-trips correctly through the role VARCHAR column for
 * both possible values: R2DBC's enum conversion is not always automatic depending on the
 * driver/version combination, so this is verified empirically rather than assumed.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@DataR2dbcTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class AppUserRepositoryTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll().block();
    }

    @Test
    void _01_ShouldReloadUserRole_WhenUserIsSaved() {
        AppUser saved = appUserRepository.save(user("ada", Role.USER)).block();

        StepVerifier.create(appUserRepository.findByUsername("ada"))
                .expectNextMatches(found -> found.getRole() == Role.USER)
                .verifyComplete();
    }

    @Test
    void _02_ShouldReloadAdminRole_WhenAdminIsSaved() {
        appUserRepository.save(user("admin", Role.ADMIN)).block();

        StepVerifier.create(appUserRepository.findByUsername("admin"))
                .expectNextMatches(found -> found.getRole() == Role.ADMIN)
                .verifyComplete();
    }

    @Test
    void _03_ShouldReflectSavedRows_WhenCheckingExistenceByUsernameAndEmail() {
        appUserRepository.save(user("ada", Role.USER)).block();

        StepVerifier.create(appUserRepository.existsByUsername("ada")).expectNext(true).verifyComplete();
        StepVerifier.create(appUserRepository.existsByUsername("missing")).expectNext(false).verifyComplete();
        StepVerifier.create(appUserRepository.existsByEmail("ada@example.com")).expectNext(true).verifyComplete();
    }

    private AppUser user(String username, Role role) {
        return AppUser.builder()
                .username(username)
                .password("hashed")
                .email(username + "@example.com")
                .role(role)
                .build();
    }
}
