package edgareldy.springwebfluxtutorial.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Locks in the reactive behavior of LoggingAspect empirically rather than by reasoning about
 * it: the Mono returned by joinPoint.proceed() must stay cold, with no eager subscription by
 * the aspect itself, and entry/exit/error logging must only fire once something downstream
 * actually subscribes.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
class LoggingAspectTest {

    private final LoggingAspect aspect = new LoggingAspect();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(LoggingAspect.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(LoggingAspect.class)).detachAppender(logAppender);
    }

    @Test
    void _01_ShouldLogOnlyOnceSubscribed_WhenPipelineIsNotSubscribedEagerly() throws Throwable {
        AtomicInteger subscriptions = new AtomicInteger();
        Mono<String> lazySource = Mono.fromSupplier(() -> {
            subscriptions.incrementAndGet();
            return "result";
        });

        ProceedingJoinPoint joinPoint = mockJoinPoint(lazySource);

        Object result = aspect.logServiceCall(joinPoint);

        assertThat(subscriptions.get()).isZero();
        assertThat(logAppender.list).isEmpty();

        StepVerifier.create((Mono<Object>) result)
                .expectNext("result")
                .verifyComplete();

        assertThat(subscriptions.get()).isEqualTo(1);
        assertThat(logAppender.list).extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("Entering"))
                .anyMatch(message -> message.contains("Exiting"));
    }

    @Test
    void _02_ShouldLogErrorWithoutSwallowingIt_WhenPipelineFailsOnSubscription() throws Throwable {
        RuntimeException boom = new RuntimeException("boom");
        ProceedingJoinPoint joinPoint = mockJoinPoint(Mono.error(boom));

        Object result = aspect.logServiceCall(joinPoint);

        assertThat(logAppender.list).isEmpty();

        StepVerifier.create((Mono<?>) result)
                .expectErrorMatches(ex -> ex == boom)
                .verify();

        assertThat(logAppender.list).extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(message -> message.contains("Exception in"));
    }

    private ProceedingJoinPoint mockJoinPoint(Mono<?> proceedResult) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("TestService.doSomething()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(proceedResult);
        return joinPoint;
    }
}
