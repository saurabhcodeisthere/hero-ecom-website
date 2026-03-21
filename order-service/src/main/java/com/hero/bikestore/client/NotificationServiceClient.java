package com.hero.bikestore.client;

import com.hero.bikestore.dto.event.OrderNotificationEvent;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * HTTP Interface for notification-service.
 *
 * One method — one endpoint.
 * The event type inside the payload drives routing on the notification-service side.
 *
 * Wired as a Spring bean in HttpClientConfig with a RestClient
 * pointed at http://notification-service (load balanced via Eureka).
 */
@HttpExchange
public interface NotificationServiceClient {

    @PostExchange("/api/v1/notifications/order-event")
    void sendOrderEvent(@RequestBody OrderNotificationEvent event);
}
