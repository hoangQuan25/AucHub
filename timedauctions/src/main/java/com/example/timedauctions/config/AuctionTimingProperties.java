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

    private boolean softCloseEnabled = true;

    @Min(1)
    private int softCloseThresholdMinutes = 10;

    @Min(1)
    private int softCloseExtensionMinutes = 5;
}