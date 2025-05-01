package com.example.timedauctions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Maps to 400 Bad Request
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidBidException extends RuntimeException {

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}