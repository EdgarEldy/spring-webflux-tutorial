package edgareldy.springwebfluxtutorial.aspect;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Logs entry, emitted values, and errors for every method on every service bean, demonstrating
 * that a reactive @Around advice cannot log around joinPoint.proceed() the way a blocking one
 * would: the returned Mono/Flux is not subscribed to yet at that point, so entry/exit logging
 * is attached via doOnSubscribe/doOnNext/doOnError instead, and only fires once Spring actually
 * subscribes to the chain.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* edgareldy.springwebfluxtutorial.service..*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSubscribe(subscription -> logEntry(signature, joinPoint.getArgs()))
                    .doOnNext(value -> log.info("Exiting {} with result {}", signature, value))
                    .doOnError(ex -> logError(signature, ex));
        }
        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnSubscribe(subscription -> logEntry(signature, joinPoint.getArgs()))
                    .doOnNext(value -> log.info("Emitting from {}: {}", signature, value))
                    .doOnError(ex -> logError(signature, ex));
        }
        return result;
    }

    private void logEntry(String signature, Object[] args) {
        log.info("Entering {} with arguments {}", signature, Arrays.toString(args));
    }

    private void logError(String signature, Throwable ex) {
        log.error("Exception in {}: {}", signature, ex.getMessage());
    }
}
