package edgareldy.springwebfluxtutorial.security;

import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reactive equivalent of Spring Security's UserDetailsService: loads an AppUser by username
 * and adapts it to a Spring Security UserDetails, with a single ROLE_&lt;role&gt; authority.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Service
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final AppUserRepository appUserRepository;

    public ReactiveUserDetailsServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return appUserRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("No user with username " + username)))
                .map(appUser -> User.builder()
                        .username(appUser.getUsername())
                        .password(appUser.getPassword())
                        .authorities("ROLE_" + appUser.getRole().name())
                        .build());
    }
}
