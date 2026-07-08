package edgareldy.springwebfluxtutorial.exception;

import java.time.Instant;
import java.util.List;

/**
 * Structured error detail placed in the data field of an ApiResponse<ErrorResponse> whenever
 * GlobalExceptionHandler handles an exception, so clients get more than just a plain message
 * string.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    /**
     * A single field validation failure, used when Bean Validation rejects a request body.
     * <p>
     * Created by edgar.muhamyangabo on 7/8/26
     * Author : edgar.muhamyangabo
     * Date : 7/8/26
     * Project : spring-webflux-tutorial
     */
    public record FieldError(String field, String message) {
    }
}
