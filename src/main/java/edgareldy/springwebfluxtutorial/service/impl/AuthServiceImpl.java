package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.auth.AuthResponse;
import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.dto.auth.UserProfileResponse;
import edgareldy.springwebfluxtutorial.entity.user.AppUser;
import edgareldy.springwebfluxtutorial.entity.user.Role;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.repository.AppUserRepository;
import edgareldy.springwebfluxtutorial.security.JwtService;
import edgareldy.springwebfluxtutorial.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of AuthService.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Chains two uniqueness checks (username, then email) before saving: each ternary branch
     * either errors with a BusinessRuleException or proceeds to the next Mono in the chain, so
     * the save only ever runs once both checks have passed.
     */
    @Override
    @Transactional
    public Mono<AuthResponse> register(RegisterRequest request) {
        return appUserRepository.existsByUsername(request.username())
                .flatMap(usernameTaken -> usernameTaken
                        ? Mono.<Boolean>error(new BusinessRuleException(
                                "Username " + request.username() + " is already taken"))
                        : appUserRepository.existsByEmail(request.email()))
                .flatMap(emailTaken -> emailTaken
                        ? Mono.<AppUser>error(new BusinessRuleException(
                                "Email " + request.email() + " is already in use"))
                        : appUserRepository.save(AppUser.builder()
                                .username(request.username())
                                .password(passwordEncoder.encode(request.password()))
                                .email(request.email())
                                .role(Role.USER)
                                .build()))
                .map(this::toAuthResponse);
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return appUserRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid username or password")))
                .flatMap(appUser -> passwordEncoder.matches(request.password(), appUser.getPassword())
                        ? Mono.just(appUser)
                        : Mono.error(new BadCredentialsException("Invalid username or password")))
                .map(this::toAuthResponse);
    }

    @Override
    public Mono<UserProfileResponse> me(String username) {
        return appUserRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with username " + username)))
                .map(appUser -> new UserProfileResponse(appUser.getUsername(), appUser.getEmail(),
                        appUser.getRole().name()));
    }

    private AuthResponse toAuthResponse(AppUser appUser) {
        String token = jwtService.generateToken(appUser.getUsername(), appUser.getRole().name());
        return new AuthResponse(token, appUser.getUsername(), appUser.getRole().name());
    }
}
