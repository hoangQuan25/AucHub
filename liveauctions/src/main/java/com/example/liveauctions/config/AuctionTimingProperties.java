package com.example.liveauctions.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auction.timing")
public class AuctionTimingProperties {

    /** If a bid arrives while time-left ≤ threshold, extend by extension seconds */
    private SoftClose softClose = new SoftClose();

    @Data public static class SoftClose {
        private boolean enabled = true;
        private long thresholdSeconds = 60;
        private long extensionSeconds = 20;
    }
}
