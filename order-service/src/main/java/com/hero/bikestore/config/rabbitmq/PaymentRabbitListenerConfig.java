package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Listener container factory for consuming payment reply events.
 *
 * WHY A SEPARATE LISTENER CONFIG?
 * payment-service and order-service are independent services with different
 * package names. When payment-service publishes PaymentResultEvent, it adds a
 * __TypeId__ header containing its own class name:
 *   com.hero.bikestore.dto.PaymentResultEvent (payment-service package)
 *
 * order-service has its own DTO at:
 *   com.hero.bikestore.dto.payment.PaymentResultEvent (different package)
 *
 * With default TYPE_ID precedence, Spring tries to load the payment-service
 * class — which doesn't exist in order-service classpath → ClassNotFoundException.
 *
 * FIX: TypePrecedence.INFERRED
 * Spring AMQP infers the target class from the @RabbitListener method parameter
 * type, ignoring the __TypeId__ header entirely.
 * The JSON fields map to order-service's PaymentResultEvent directly.
 *
 * setDefaultRequeueRejected(false):
 * If PaymentReplyListener throws an exception, the message is discarded
 * instead of requeueing — prevents infinite retry loop.
 *
 * Reason to change: only if serialization strategy changes.
 */
@Configuration
public class PaymentRabbitListenerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

        // INFERRED: use the @RabbitListener method parameter type for deserialization
        // Ignores __TypeId__ header — no cross-service class name dependency
        converter.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
