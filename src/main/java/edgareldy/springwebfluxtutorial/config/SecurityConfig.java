package edgareldy.springwebfluxtutorial.config;

import edgareldy.springwebfluxtutorial.security.JwtAccessDeniedHandler;
import edgareldy.springwebfluxtutorial.security.JwtAuthWebFilter;
import edgareldy.springwebfluxtutorial.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Declares the reactive security filter chain and the per-resource authorization rules from
 * the README: categories/products readable by anyone, writable only by ADMIN; customers/orders
 * readable by any authenticated user, writable only by ADMIN; the order status stream readable
 * by any authenticated user. Stateless (no session, no CSRF): every request authenticates
 * itself via its own Bearer token.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] ADMIN_WRITE_PATHS = {
            "/api/v1/categories/**", "/api/v1/products/**", "/api/v1/customers/**", "/api/v1/orders/**"
    };

    private final JwtAuthWebFilter jwtAuthWebFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthWebFilter jwtAuthWebFilter, JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthWebFilter = jwtAuthWebFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Rule order matters: authorizeExchange evaluates rules top to bottom and applies the
     * first match, so the specific public GET rules for categories/products must come before
     * the generic write rule for the same paths, which must in turn come before the
     * anyExchange() catch-all.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**").permitAll()
                        .pathMatchers(HttpMethod.POST, ADMIN_WRITE_PATHS).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, ADMIN_WRITE_PATHS).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, ADMIN_WRITE_PATHS).hasRole("ADMIN")
                        .anyExchange().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAt(jwtAuthWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
