package com.hero.bikestore.publisher;

import com.hero.bikestore.config.rabbitmq.PaymentCommandPublisherConfig;
import com.hero.bikestore.dto.payment.ProcessPaymentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of PaymentCommandPublisher.
 *
 * Publishes ProcessPaymentCommand to payment.commands exchange.
 * Routing key is fixed: "payment.process" — there is only one type of
 * payment command, so no dynamic routing key is needed here.
 *
 * SRP: Only publishes the command. No business logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQPaymentCommandPublisher implements PaymentCommandPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(ProcessPaymentCommand command) {
        log.info("Publishing payment command — exchange={} routingKey={} orderId={}",
                PaymentCommandPublisherConfig.EXCHANGE_NAME,
                PaymentCommandPublisherConfig.ROUTING_KEY,
                command.getOrderId());

        rabbitTemplate.convertAndSend(
                PaymentCommandPublisherConfig.EXCHANGE_NAME,
                PaymentCommandPublisherConfig.ROUTING_KEY,
                command
        );

        log.info("Payment command published — orderId={} amount={}",
                command.getOrderId(), command.getAmount());
    }
}
