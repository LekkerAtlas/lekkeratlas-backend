package nl.lekkeratlas.shared.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ exchange, queue and binding.
 * <p>
 * Declaring this in both backend and worker is okay as long as the definitions
 * remain identical. RabbitMQ declarations are idempotent when they match.
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    DirectExchange workExchange() {
        return new DirectExchange(
                RabbitNames.WORK_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    Queue workQueue() {
        return QueueBuilder
                .durable(RabbitNames.WORK_QUEUE)
                .build();
    }

    @Bean
    Binding workBinding(
            Queue workQueue,
            DirectExchange workExchange
    ) {
        return BindingBuilder
                .bind(workQueue)
                .to(workExchange)
                .with(RabbitNames.WORK_ROUTING_KEY);
    }
}