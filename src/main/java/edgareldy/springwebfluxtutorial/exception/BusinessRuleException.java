package edgareldy.springwebfluxtutorial.exception;

/**
 * Thrown by service implementations when an operation violates a domain rule (e.g. deleting a
 * Category that still has Products attached). Caught by GlobalExceptionHandler and translated
 * into a 422 response.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
