package com.cubecrush.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {
    private final String localizationKey;
    private final HttpStatus status;

    public AuthException(String localizationKey, HttpStatus status) {
        super(localizationKey);
        this.localizationKey = localizationKey;
        this.status = status;
    }

    public AuthException(String localizationKey, HttpStatus status, String message) {
        super(message);
        this.localizationKey = localizationKey;
        this.status = status;
    }
}
