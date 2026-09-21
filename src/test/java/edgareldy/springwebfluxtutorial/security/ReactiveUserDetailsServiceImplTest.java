package edgareldy.springwebfluxtutorial.security;

import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

/**
 * Unit tests for ReactiveUserDetailsServiceImpl, in particular that the ROLE_ prefix and
 * authority are derived correctly from AppUser.role.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ReactiveUserDetailsServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    private ReactiveUserDetailsServiceImpl userDetailsService;

    @Test
    void _01_ShouldReturnUserDetailsWithRolePrefixedAuthority_WhenUsernameExists() {
        userDetailsService = new ReactiveUserDetailsServiceImpl(appUserRepository);
        AppUser appUser = AppUser.builder().id(1L).username("ada").password("hashed").email("ada@example.com")
                .role(Role.ADMIN).build();
        when(appUserRepository.findByUsername("ada")).thenReturn(Mono.just(appUser));

        StepVerifier.create(userDetailsService.findByUsername("ada"))
                .expectNextMatches((UserDetails userDetails) -> userDetails.getUsername().equals("ada")
                        && userDetails.getPassword().equals("hashed")
                        && userDetails.getAuthorities().stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))
                .verifyComplete();
    }

    @Test
    void _02_ShouldErrorWithUsernameNotFound_WhenUsernameIsMissing() {
        userDetailsService = new ReactiveUserDetailsServiceImpl(appUserRepository);
        when(appUserRepository.findByUsername("missing")).thenReturn(Mono.empty());

        StepVerifier.create(userDetailsService.findByUsername("missing"))
                .expectError(UsernameNotFoundException.class)
                .verify();
    }
}
