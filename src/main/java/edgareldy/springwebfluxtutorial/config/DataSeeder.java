package edgareldy.springwebfluxtutorial.config;

import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds a default admin account on startup, active only under the "dev" profile. Without this,
 * AuthServiceImpl.register() always assigns Role.USER, and this project has no other way to
 * obtain an ADMIN account: every write endpoint (categories/products/customers/orders)
 * requires ADMIN, so there would be no way to reach them at all through the public API. Skips
 * seeding entirely if any user already exists, so restarting the app against a persistent dev
 * database does not duplicate rows.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Configuration
@Profile("dev")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * CommandLineRunner is a plain synchronous callback, not part of any reactive chain a
     * caller subscribes to, so subscribe() is the correct way to trigger the pipeline here,
     * the same reasoning as StockReportScheduler's @Scheduled method.
     */
    @Bean
    public CommandLineRunner seedAdminUser() {
        return args -> appUserRepository.count()
                .filter(count -> count == 0)
                .flatMap(count -> appUserRepository.save(AppUser.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin1234"))
                        .email("admin@example.com")
                        .role(Role.ADMIN)
                        .build()))
                .subscribe(
                        saved -> log.info("Seeded default admin user (username: admin)"),
                        ex -> log.error("Failed to seed default admin user", ex));
    }
}
