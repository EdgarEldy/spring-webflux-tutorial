package edgareldy.springwebfluxtutorial.security;

import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive equivalent of a servlet OncePerRequestFilter: reads the Bearer token from the
 * Authorization header, validates it, and if valid publishes an Authentication into the
 * reactive security context via contextWrite rather than a ThreadLocal, since a single request
 * can hop between event-loop threads in WebFlux. A missing or invalid token is not an error
 * here: the request simply proceeds unauthenticated, and SecurityConfig's per-route rules
 * decide whether that is enough.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Component
public class JwtAuthWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtService.isValid(token)) {
            return chain.filter(exchange);
        }

        String username = jwtService.extractUsername(token);
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + jwtService.extractRole(token)));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
}
