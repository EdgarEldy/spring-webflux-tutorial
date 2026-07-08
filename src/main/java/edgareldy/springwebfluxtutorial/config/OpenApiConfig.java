package edgareldy.springwebfluxtutorial.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the Swagger UI metadata (title, description, version) and the JWT bearer security
 * scheme, so the "Authorize" button in Swagger UI can attach a token to protected requests
 * once feature/auth adds Spring Security. Served via springdoc-openapi-starter-webflux-ui, the
 * reactive variant of springdoc.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI springWebfluxTutorialOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring WebFlux Tutorial API")
                        .description("Reactive REST API demonstrating Spring WebFlux: "
                                + "categories, products, customers, orders and JWT authentication.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
