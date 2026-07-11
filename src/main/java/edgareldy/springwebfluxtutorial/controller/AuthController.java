package edgareldy.springwebfluxtutorial.controller;

import edgareldy.springwebfluxtutorial.dto.auth.AuthResponse;
import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.dto.auth.UserProfileResponse;
import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Annotated REST controller for authentication, delegating all logic to AuthService.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request)
                .map(result -> ApiResponse.success(result, "Registration successful"));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(result -> ApiResponse.success(result, "Login successful"));
    }

    /**
     * The principal is the raw username String, set that way by JwtAuthWebFilter rather than
     * a full UserDetails object, so @AuthenticationPrincipal String resolves it directly with
     * no getName()/toString() ambiguity to worry about.
     */
    @GetMapping("/me")
    public Mono<ApiResponse<UserProfileResponse>> me(@AuthenticationPrincipal String username) {
        return authService.me(username)
                .map(result -> ApiResponse.success(result, "Profile retrieved"));
    }
}
