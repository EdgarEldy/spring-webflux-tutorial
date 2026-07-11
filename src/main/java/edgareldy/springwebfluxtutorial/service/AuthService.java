package edgareldy.springwebfluxtutorial.service;

import edgareldy.springwebfluxtutorial.dto.auth.AuthResponse;
import edgareldy.springwebfluxtutorial.dto.auth.LoginRequest;
import edgareldy.springwebfluxtutorial.dto.auth.RegisterRequest;
import edgareldy.springwebfluxtutorial.dto.auth.UserProfileResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive contract for authentication operations. AuthController depends on this interface,
 * never on AuthServiceImpl directly.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public interface AuthService {

    Mono<AuthResponse> register(RegisterRequest request);

    Mono<AuthResponse> login(LoginRequest request);

    Mono<UserProfileResponse> me(String username);
}
