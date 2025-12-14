package com.cubecrush.user.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserException extends RuntimeException {
    private final String localizationKey;
    private final HttpStatus status;

    public UserException(String localizationKey, HttpStatus status) {
        super(localizationKey);
        this.localizationKey = localizationKey;
        this.status = status;
    }

    public UserException(String localizationKey, HttpStatus status, String message) {
        super(message);
        this.localizationKey = localizationKey;
        this.status = status;
    }
}
