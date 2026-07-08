package edgareldy.springwebfluxtutorial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's @Scheduled support so scheduled beans (e.g. StockReportScheduler on
 * feature/orders) can run periodic jobs without any further wiring.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
