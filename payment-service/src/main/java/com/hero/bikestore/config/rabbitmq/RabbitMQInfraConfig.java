package com.hero.bikestore.config.rabbitmq;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SRP: Only responsible for RabbitMQ infrastructure wiring.
 *
 * Two concerns owned here:
 *
 * 1. MESSAGE CONVERTER
 *    JacksonJsonMessageConverter — serializes/deserializes Java objects to/from JSON.
 *    Without this, Spring AMQP uses Java serialization (brittle, version-sensitive).
 *    JSON works across any language and is human-readable in the RabbitMQ dashboard.
 *
 * 2. LISTENER CONTAINER FACTORY
 *    Wires the converter into the @RabbitListener infrastructure.
 *    setDefaultRequeueRejected(false) — if PaymentCommandListener throws an exception,
 *    the message is NOT requeued. It is discarded (or sent to DLQ if configured).
 *    Without this, a bad message causes an infinite retry loop.
 *
 * Reason to change this class:
 *    Only if serialization format changes (e.g. JSON → Protobuf)
 *    or ACK/requeue strategy changes.
 *    Queue shapes and exchange names never touch this class.
 */
@Configuration
public class RabbitMQInfraConfig {

    /**
     * RabbitAdmin — forces Spring AMQP to declare all Exchange, Queue and Binding
     * beans on startup rather than lazily (which only happens on first listener use).
     *
     * Without this, payment.commands and payment.events exchanges won't appear in
     * RabbitMQ until the first @RabbitListener connects — which can cause order-service
     * to fail publishing if it starts before payment-service has declared its exchanges.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Forces RabbitAdmin to declare all Exchange, Queue and Binding beans
     * immediately after startup — without waiting for a @RabbitListener to trigger
     * the first connection.
     *
     * WHY THIS IS NEEDED:
     * Spring AMQP connections are lazy. Without a @RabbitListener registered,
     * no physical connection to RabbitMQ is made at startup, so RabbitAdmin
     * never gets a chance to declare exchanges/queues.
     * This is a bootstrap issue — once Phase 4 adds @RabbitListener, the
     * connection will be established automatically and this runner becomes a
     * safe no-op (initialize() is idempotent).
     */
    @Bean
    public ApplicationRunner rabbitInitializer(RabbitAdmin rabbitAdmin) {
        return args -> rabbitAdmin.initialize();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        // false → failed message is NOT requeued → prevents infinite retry loop
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
}
