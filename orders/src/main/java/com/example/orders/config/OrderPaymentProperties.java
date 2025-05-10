package com.example.orders.config; // Suggested package

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "orders-config.payment.timeout")
@Getter
@Setter
public class OrderPaymentProperties {
    private Duration liveAuctionWinnerDuration = Duration.ofHours(2);
    private Duration timedAuctionWinnerDuration = Duration.ofHours(24);
    private Duration nextBidderDuration = Duration.ofHours(24);
}