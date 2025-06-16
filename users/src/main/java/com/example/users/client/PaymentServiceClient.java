package com.example.users.client;

import com.example.users.client.dto.ConfirmStripePaymentMethodRequestClientDto;
import com.example.users.client.dto.CreateStripeSetupIntentRequestClientDto;
import com.example.users.client.dto.CreateStripeSetupIntentResponseClientDto;
import com.example.users.client.dto.StripePaymentMethodDetailsClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payments")
public interface PaymentServiceClient {

    @PostMapping("/setup-intent")
    ResponseEntity<CreateStripeSetupIntentResponseClientDto> createStripeSetupIntent(
            @RequestBody CreateStripeSetupIntentRequestClientDto requestDto);

    @PostMapping("/confirm-payment-method")
    ResponseEntity<StripePaymentMethodDetailsClientDto> confirmAndSaveStripePaymentMethod(
            @RequestBody ConfirmStripePaymentMethodRequestClientDto requestDto);
}
