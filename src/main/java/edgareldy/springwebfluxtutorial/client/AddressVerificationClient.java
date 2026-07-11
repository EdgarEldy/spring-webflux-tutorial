package edgareldy.springwebfluxtutorial.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Demonstrates a reactive HTTP call to an external service (WebClientConfig's shared builder)
 * with timeout and error handling. Address verification is best effort, never a reason to
 * fail an order: onErrorResume treats a timeout, a network error, or a non-2xx response
 * identically, all fail open rather than blocking the caller.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Component
public class AddressVerificationClient {

    private static final Logger log = LoggerFactory.getLogger(AddressVerificationClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final WebClient webClient;

    public AddressVerificationClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://address-verification.invalid").build();
    }

    /**
     * Fails open (returns true) on timeout or any error, since this project has no real
     * address verification provider to call and an order should never be blocked by a flaky
     * or unreachable third-party service.
     */
    public Mono<Boolean> verify(String address) {
        return webClient.get()
                .uri("/verify?address={address}", address)
                .retrieve()
                .bodyToMono(Boolean.class)
                .timeout(TIMEOUT)
                .onErrorResume(ex -> {
                    log.warn("Address verification unavailable, proceeding anyway: {}", ex.getMessage());
                    return Mono.just(true);
                });
    }
}
