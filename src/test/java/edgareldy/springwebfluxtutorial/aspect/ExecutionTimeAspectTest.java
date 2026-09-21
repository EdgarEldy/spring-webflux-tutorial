package edgareldy.springwebfluxtutorial.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks in the reactive behavior of ExecutionTimeAspect empirically: the clock must start when
 * the returned Mono is actually subscribed to, not at the moment joinPoint.proceed() returns,
 * otherwise a slow downstream call would be measured as instantaneous.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
class ExecutionTimeAspectTest {

    private final ExecutionTimeAspect aspect = new ExecutionTimeAspect();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(ExecutionTimeAspect.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(ExecutionTimeAspect.class)).detachAppender(logAppender);
    }

    @Test
    void _01_ShouldMeasureElapsedTimeFromSubscription_WhenPipelineIsSubscribed() throws Throwable {
        Duration delay = Duration.ofMillis(50);
        ProceedingJoinPoint joinPoint = mockJoinPoint(Mono.just("done").delayElement(delay));

        long proceedStart = System.nanoTime();
        Object result = aspect.logExecutionTime(joinPoint);
        long proceedElapsedNanos = System.nanoTime() - proceedStart;

        assertThat(logAppender.list).isEmpty();
        assertThat(proceedElapsedNanos).isLessThan(delay.toNanos());

        StepVerifier.create((Mono<Object>) result)
                .expectNext("done")
                .verifyComplete();

        // doFinally runs on the delayElement's scheduler thread, slightly after verifyComplete()
        // observes the completion signal on the main thread, so poll briefly instead of asserting
        // immediately.
        awaitLogMessage();

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getFormattedMessage()).contains("executed in");
    }

    private void awaitLogMessage() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1_000;
        while (logAppender.list.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    private ProceedingJoinPoint mockJoinPoint(Mono<?> proceedResult) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("TestController.handle()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(proceedResult);
        return joinPoint;
    }
}
