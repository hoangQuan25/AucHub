package com.example.payments.config; // In your Payment Service

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Getter
@Configuration
// Option 1: Using @Value directly (simpler for just one key)
public class StripeConfig {

    // Getter if needed by services directly
    @Value("${stripe.api.secret-key}")
    private String secretKey;

    // The webhook signing secret will be needed later in the WebhookController/Service
    @Value("${stripe.webhook.signing-secret}")
    // Make it accessible for webhook handler
    private String webhookSigningSecret;


    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = secretKey;
        // You can also set other global Stripe settings here if needed, like API version
        // Stripe.setApiVersion("2022-11-15"); // Example: Pin to a specific API version
        // log.info("Stripe API Key Initialized."); // Add logging if desired
    }

}