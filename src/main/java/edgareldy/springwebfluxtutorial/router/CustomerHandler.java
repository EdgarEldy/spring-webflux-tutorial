package edgareldy.springwebfluxtutorial.router;

import edgareldy.springwebfluxtutorial.dto.common.ApiResponse;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerRequest;
import edgareldy.springwebfluxtutorial.service.CustomerService;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * Functional handler for Customer, the alternative to feature/products' annotated controller
 * style. One method per action, each returning Mono&lt;ServerResponse&gt;, with validation
 * done manually via an injected Validator since @Valid does not apply to functional endpoints.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Component
public class CustomerHandler {

    private final CustomerService customerService;
    private final Validator validator;

    public CustomerHandler(CustomerService customerService, Validator validator) {
        this.customerService = customerService;
        this.validator = validator;
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        int page = parseIntParam(request, "page", 0);
        int size = parseIntParam(request, "size", 20);
        String search = request.queryParam("search").orElse(null);

        return customerService.findAll(page, size, search)
                .flatMap(result -> ServerResponse.ok().bodyValue(ApiResponse.success(result, "Customers retrieved")));
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        Long id = parseId(request);

        return customerService.findById(id)
                .flatMap(result -> ServerResponse.ok().bodyValue(ApiResponse.success(result, "Customer retrieved")));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(CustomerRequest.class)
                .doOnNext(this::validate)
                .flatMap(customerService::create)
                .flatMap(result -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(ApiResponse.success(result, "Customer created")));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        Long id = parseId(request);

        return request.bodyToMono(CustomerRequest.class)
                .doOnNext(this::validate)
                .flatMap(body -> customerService.update(id, body))
                .flatMap(result -> ServerResponse.ok().bodyValue(ApiResponse.success(result, "Customer updated")));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        Long id = parseId(request);

        return customerService.delete(id)
                .then(ServerResponse.ok().bodyValue(ApiResponse.<Void>success(null, "Customer deleted")));
    }

    /**
     * Manual equivalent of @Valid: runs Bean Validation against the injected Validator and
     * raises a ServerWebInputException (mapped to 400 by GlobalExceptionHandler) carrying the
     * field errors as its reason, since functional endpoints get no automatic validation.
     */
    private void validate(CustomerRequest request) {
        Errors errors = new BeanPropertyBindingResult(request, CustomerRequest.class.getName());
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            String message = errors.getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            throw new ServerWebInputException(message);
        }
    }

    /**
     * Manual equivalent of Spring's automatic @PathVariable Long conversion: an annotated
     * controller would already reject a non-numeric id with a 400 before the method body ever
     * runs, so a malformed id here must raise the same ServerWebInputException instead of
     * letting NumberFormatException fall through to the generic 500 branch.
     */
    private Long parseId(ServerRequest request) {
        String rawId = request.pathVariable("id");
        try {
            return Long.valueOf(rawId);
        } catch (NumberFormatException ex) {
            throw new ServerWebInputException("id must be a number, got: " + rawId);
        }
    }

    private int parseIntParam(ServerRequest request, String name, int defaultValue) {
        return request.queryParam(name)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ex) {
                        throw new ServerWebInputException(name + " must be a number, got: " + value);
                    }
                })
                .orElse(defaultValue);
    }
}
