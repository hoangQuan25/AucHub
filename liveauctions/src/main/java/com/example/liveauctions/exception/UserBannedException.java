package com.example.liveauctions.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}