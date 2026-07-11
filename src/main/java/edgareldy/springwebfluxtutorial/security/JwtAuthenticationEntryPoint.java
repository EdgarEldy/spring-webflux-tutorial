package edgareldy.springwebfluxtutorial.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Re-raises the AuthenticationException instead of writing a response directly, so it
 * propagates past Spring Security's WebFilter chain to GlobalExceptionHandler, which already
 * maps AuthenticationException to 401 with this project's standard ApiResponse envelope: one
 * place decides what an error response looks like, not two.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Component
public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return Mono.error(ex);
    }
}
