package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for AppUser. Used directly by AuthServiceImpl (login lookup,
 * registration uniqueness checks) and by DataSeeder. Also used by
 * ReactiveUserDetailsServiceImpl, which exists as a demonstration of the
 * ReactiveUserDetailsService contract but is not currently wired into the request-handling
 * chain: JwtAuthWebFilter builds the Authentication straight from the JWT's own claims and
 * never consults it, since this project only supports httpBasic/formLogin disabled, JWT-only
 * authentication.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public interface AppUserRepository extends ReactiveCrudRepository<AppUser, Long> {

    Mono<AppUser> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    Mono<Boolean> existsByEmail(String email);
}
