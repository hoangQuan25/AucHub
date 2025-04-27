package com.example.liveauctions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Maps to 400 Bad Request
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidAuctionStateException extends RuntimeException {

    public InvalidAuctionStateException(String message) {
        super(message);
    }

    public InvalidAuctionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}