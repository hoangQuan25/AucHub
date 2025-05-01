package com.example.timedauctions.exception; // Your exception package

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Annotation helps map this directly to 404 if not caught by @ControllerAdvice
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class AuctionNotFoundException extends RuntimeException {

    public AuctionNotFoundException(String message) {
        super(message);
    }

    public AuctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}