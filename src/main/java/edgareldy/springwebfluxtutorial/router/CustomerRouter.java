package edgareldy.springwebfluxtutorial.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Declares the Customer routes as a RouterFunction bean, delegating each route to the matching
 * CustomerHandler method, the functional-endpoints counterpart to feature/products' annotated
 * @RestControllers.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Configuration
public class CustomerRouter {

    private static final String BASE_PATH = "/api/v1/customers";
    private static final String ID_PATH = BASE_PATH + "/{id}";

    @Bean
    public RouterFunction<ServerResponse> customerRoutes(CustomerHandler customerHandler) {
        return RouterFunctions.route()
                .GET(BASE_PATH, customerHandler::findAll)
                .GET(ID_PATH, customerHandler::findById)
                .POST(BASE_PATH, customerHandler::create)
                .PUT(ID_PATH, customerHandler::update)
                .DELETE(ID_PATH, customerHandler::delete)
                .build();
    }
}
