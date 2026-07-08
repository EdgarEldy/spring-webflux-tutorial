package edgareldy.springwebfluxtutorial.aspect;

import java.util.concurrent.atomic.AtomicLong;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Measures how long each controller or router/handler method takes to actually run, from
 * subscription to completion, cancellation or error, covering both API styles used in this
 * project. The clock starts in doOnSubscribe and stops in doFinally, since starting it at the
 * moment joinPoint.proceed() returns would only measure how long it took to build the
 * Mono/Flux pipeline, not how long the underlying work actually took.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Aspect
@Component
public class ExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("execution(* edgareldy.springwebfluxtutorial.controller..*(..)) "
            + "|| execution(* edgareldy.springwebfluxtutorial.router..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        Object result = joinPoint.proceed();
        AtomicLong startNanos = new AtomicLong();

        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSubscribe(subscription -> startNanos.set(System.nanoTime()))
                    .doFinally(signalType -> logElapsed(signature, startNanos.get(), signalType.toString()));
        }
        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnSubscribe(subscription -> startNanos.set(System.nanoTime()))
                    .doFinally(signalType -> logElapsed(signature, startNanos.get(), signalType.toString()));
        }
        return result;
    }

    private void logElapsed(String signature, long startNanos, String signalType) {
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("{} executed in {} ms ({})", signature, elapsedMillis, signalType);
    }
}
