package com.example.timedauctions.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


@Configuration
@ConfigurationProperties(prefix = "auction.timing")
@Getter @Setter
@Validated // Enable validation on properties
public class AuctionTimingProperties {

    @NotNull
    private SoftClose softClose = new SoftClose();

    // Fast finish might not be relevant, but keep structure if needed
    // private FastFinish fastFinish = new FastFinish();

    @Getter @Setter
    public static class SoftClose {
        private boolean enabled = true;

        @Min(1)
        private int thresholdMinutes = 10; // Default threshold in minutes

        @Min(1)
        private int extensionMinutes = 5; // Default extension in minutes
    }

}