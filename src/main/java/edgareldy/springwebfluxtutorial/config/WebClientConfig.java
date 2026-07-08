package edgareldy.springwebfluxtutorial.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Exposes a reusable, pre-configured WebClient.Builder bean, so every downstream reactive HTTP
 * call in the project (e.g. the external service demonstration on feature/orders) shares the
 * same connect and response timeouts instead of each caller building its own client.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(RESPONSE_TIMEOUT);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
