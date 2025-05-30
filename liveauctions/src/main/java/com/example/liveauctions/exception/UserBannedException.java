package com.example.liveauctions.exception; // Adjust package

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // Or any other appropriate status
public class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}