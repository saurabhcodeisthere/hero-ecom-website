package com.hero.bikestore.client;

import com.hero.bikestore.client.response.UserAddressClientResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HTTP Interface client for user-service.
 *
 * Called at checkout when the customer provides an addressId instead of
 * typing the full address inline. cart-service fetches the full address
 * from user-service and forwards it to order-service.
 *
 * The customer's JWT is forwarded as the Authorization header because
 * user-service requires authentication on all /api/v1/users/** paths.
 * The keycloakUserId is also passed as a query param so user-service
 * can verify the address belongs to the requesting customer.
 */
@HttpExchange
public interface UserServiceClient {

    @GetExchange("/api/v1/users/addresses/{addressId}")
    UserAddressClientResponse getAddressById(
            @PathVariable Long addressId,
            @RequestParam("userId") String keycloakUserId,
            @RequestHeader("Authorization") String bearerToken);
}
