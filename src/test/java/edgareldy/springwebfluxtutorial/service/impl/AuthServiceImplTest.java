package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.dto.auth.UserProfileResponse;
import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import edgareldy.springwebfluxtutorial.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthServiceImpl: the username/email uniqueness checks on register(), password
 * matching on login(), and that neither ever logs or returns a plaintext password.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(appUserRepository, passwordEncoder, jwtService);
    }

    @Test
    void _01_ShouldSaveUserWithEncodedPasswordAndReturnToken_WhenUserRegisters() {
        RegisterRequest request = new RegisterRequest("ada", "supersecret", "ada@example.com");
        AppUser saved = AppUser.builder().id(1L).username("ada").password("hashed").email("ada@example.com")
                .role(Role.USER).build();

        when(appUserRepository.existsByUsername("ada")).thenReturn(Mono.just(false));
        when(appUserRepository.existsByEmail("ada@example.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("supersecret")).thenReturn("hashed");
        when(appUserRepository.save(org.mockito.ArgumentMatchers.any(AppUser.class))).thenReturn(Mono.just(saved));
        when(jwtService.generateToken("ada", "USER")).thenReturn("token123");

        StepVerifier.create(authService.register(request))
                .expectNextMatches(response -> response.token().equals("token123")
                        && response.username().equals("ada")
                        && response.role().equals("USER"))
                .verifyComplete();
    }

    @Test
    void _02_ShouldErrorWithBusinessRule_WhenUsernameIsTaken() {
        RegisterRequest request = new RegisterRequest("ada", "supersecret", "ada@example.com");
        when(appUserRepository.existsByUsername("ada")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(request))
                .expectError(BusinessRuleException.class)
                .verify();
    }

    @Test
    void _03_ShouldErrorWithBusinessRule_WhenEmailIsTaken() {
        RegisterRequest request = new RegisterRequest("ada", "supersecret", "ada@example.com");
        when(appUserRepository.existsByUsername("ada")).thenReturn(Mono.just(false));
        when(appUserRepository.existsByEmail("ada@example.com")).thenReturn(Mono.just(true));

        StepVerifier.create(authService.register(request))
                .expectError(BusinessRuleException.class)
                .verify();
    }

    @Test
    void _04_ShouldReturnToken_WhenPasswordMatches() {
        LoginRequest request = new LoginRequest("ada", "supersecret");
        AppUser appUser = AppUser.builder().id(1L).username("ada").password("hashed").email("ada@example.com")
                .role(Role.ADMIN).build();

        when(appUserRepository.findByUsername("ada")).thenReturn(Mono.just(appUser));
        when(passwordEncoder.matches("supersecret", "hashed")).thenReturn(true);
        when(jwtService.generateToken("ada", "ADMIN")).thenReturn("token456");

        StepVerifier.create(authService.login(request))
                .expectNextMatches(response -> response.token().equals("token456") && response.role().equals("ADMIN"))
                .verifyComplete();
    }

    @Test
    void _05_ShouldErrorWithBadCredentials_WhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("ada", "wrong-password");
        AppUser appUser = AppUser.builder().id(1L).username("ada").password("hashed").email("ada@example.com")
                .role(Role.USER).build();

        when(appUserRepository.findByUsername("ada")).thenReturn(Mono.just(appUser));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        StepVerifier.create(authService.login(request))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    void _06_ShouldErrorWithBadCredentials_WhenUsernameIsMissing() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login(new LoginRequest("ghost", "whatever")))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    void _07_ShouldReturnProfile_WhenUserIsFound() {
        AppUser appUser = AppUser.builder().id(1L).username("ada").password("hashed").email("ada@example.com")
                .role(Role.USER).build();
        when(appUserRepository.findByUsername("ada")).thenReturn(Mono.just(appUser));

        StepVerifier.create(authService.me("ada"))
                .expectNext(new UserProfileResponse("ada", "ada@example.com", "USER"))
                .verifyComplete();
    }

    @Test
    void _08_ShouldErrorWithResourceNotFound_WhenUserIsMissing() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(authService.me("ghost"))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }
}
